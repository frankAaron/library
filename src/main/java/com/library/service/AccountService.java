package com.library.service;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.DepositRecordMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.UserMapper;
import com.library.entity.DepositRecord;
import com.library.entity.FineRecord;
import com.library.entity.User;
import com.library.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 账户服务（押金管理 + 超期罚款缴纳闭环）
 * <p>
 * 罚款缴纳：从押金余额扣款 → 罚款单置为已缴 → 记录押金流水 → 失效用户缓存，
 * 缴清后自动解除借阅限制（自动解冻），形成"超期计费→缴费→解除限制"完整闭环。
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    /** 单次押金充值上限（元） */
    private static final BigDecimal MAX_PAY_AMOUNT = new BigDecimal("10000");
    /** 支付宝押金充值订单幂等锁前缀（防同步+异步双回调重复入账） */
    private static final String PAY_DEP_LOCK_PREFIX = "pay:dep:done:";
    /** 幂等锁保留时长（秒）：24小时内同一订单号只入账一次 */
    private static final int PAY_DEP_LOCK_TTL = 86400;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private DepositRecordMapper depositRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private NotificationService notificationService;

    /** 我的罚款列表 */
    public List<FineRecord> myFines(Long userId) {
        return fineRecordMapper.selectMy(userId);
    }

    /** 我的押金流水 */
    public List<DepositRecord> myDepositRecords(Long userId) {
        return depositRecordMapper.selectMy(userId);
    }

    /** 管理端：罚款对账分页查询 */
    public Map<String, Object> pageFines(Integer status, Integer pageNum, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int offset = (num - 1) * size;
        List<FineRecord> list = fineRecordMapper.selectPageAdmin(status, offset, size);
        long total = fineRecordMapper.countAdmin(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        result.put("pageNum", num);
        result.put("pageSize", size);
        return result;
    }

    /**
     * 读者缴纳超期罚款（从押金余额扣款）
     * 事务保证：押金扣减、罚款核销、流水记录三者一致
     */
    @Transactional
    public Result payFine(User sessionUser, Long fineId) {
        FineRecord fine = fineRecordMapper.selectById(fineId);
        if (fine == null) {
            return Result.fail("罚款记录不存在");
        }
        if (!fine.getUserId().equals(sessionUser.getId())) {
            return Result.fail("无权操作他人的罚款记录");
        }
        if (fine.getStatus() != Constants.FINE_UNPAID) {
            return Result.fail("该罚款已缴纳，请勿重复操作");
        }
        // 取最新押金余额（缓存）
        User user = permissionService.getUserCached(sessionUser.getId());
        BigDecimal balance = user.getDeposit() == null ? BigDecimal.ZERO : user.getDeposit();
        if (balance.compareTo(fine.getAmount()) < 0) {
            return Result.fail(String.format("押金余额不足（需缴罚款%s元，当前押金余额%s元），请先充值押金",
                    fine.getAmount().stripTrailingZeros().toPlainString(),
                    balance.stripTrailingZeros().toPlainString()));
        }
        // 1. 押金扣款（SQL 层原子性校验防并发余额负数）
        int rows = userMapper.updateDeposit(sessionUser.getId(), fine.getAmount().negate());
        if (rows == 0) {
            return Result.fail("押金余额不足，请先充值押金");
        }
        // 2. 罚款核销（仅未缴状态生效，防止并发重复缴费）
        if (fineRecordMapper.pay(fineId, sessionUser.getId()) == 0) {
            throw new IllegalStateException("罚款状态已变更，请刷新后重试");
        }
        // 3. 押金流水
        DepositRecord record = new DepositRecord();
        record.setUserId(sessionUser.getId());
        record.setAmount(fine.getAmount());
        record.setType(Constants.DEPOSIT_DEDUCT);
        String remarkText = "超期罚款扣款：" + (fine.getBookName() == null ? "图书借阅" : "《" + fine.getBookName() + "》");
        record.setRemark(remarkText.length() > 180 ? remarkText.substring(0, 180) : remarkText);
        depositRecordMapper.insert(record);
        // 4. 失效用户缓存（缴清罚款后借阅限制自动解除）
        permissionService.evictUserCache(sessionUser.getId());
        // 5. 若罚款已全部缴清且无长期超期未还，自动解冻账号
        tryUnfreeze(sessionUser.getId());
        return Result.ok("缴费成功，超期限制已解除，可正常借阅图书");
    }

    /**
     * 押金充值
     */
    @Transactional
    public Result payDeposit(User sessionUser, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于0");
        }
        if (amount.compareTo(MAX_PAY_AMOUNT) > 0) {
            return Result.fail("单次充值金额不能超过" + MAX_PAY_AMOUNT.stripTrailingZeros().toPlainString() + "元");
        }
        userMapper.updateDeposit(sessionUser.getId(), amount);
        DepositRecord record = new DepositRecord();
        record.setUserId(sessionUser.getId());
        record.setAmount(amount);
        record.setType(Constants.DEPOSIT_PAY);
        record.setRemark("押金充值");
        depositRecordMapper.insert(record);
        permissionService.evictUserCache(sessionUser.getId());
        User fresh = userMapper.selectById(sessionUser.getId());
        return Result.ok("充值成功！当前押金余额："
                + (fresh.getDeposit() == null ? "0" : fresh.getDeposit().stripTrailingZeros().toPlainString()) + "元");
    }

    /**
     * 管理端人工核销罚款（对账场景：线下收款后标记已缴）
     */
    @Transactional
    public Result adminMarkPaid(Long fineId, Long adminId) {
        if (fineRecordMapper.markPaidByAdmin(fineId, adminId) == 0) {
            return Result.fail("罚款记录不存在或已缴纳");
        }
        FineRecord fine = fineRecordMapper.selectById(fineId);
        if (fine != null) {
            tryUnfreeze(fine.getUserId());
        }
        return Result.ok("已标记为已缴纳");
    }

    /**
     * 缴清罚款后自动解冻：
     * 无未缴罚款 且 无超期≥15天未归还记录 且 账号当前处于冻结状态 时恢复为正常
     */
    private void tryUnfreeze(Long userId) {
        try {
            if (fineRecordMapper.countUnpaidByUser(userId) > 0) {
                return;
            }
            if (borrowRecordMapper.countLongOverdueByUser(userId, Constants.OVERDUE_FREEZE_DAYS) > 0) {
                return;
            }
            User u = userMapper.selectById(userId);
            if (u == null || u.getStatus() == null || u.getStatus() != 1) {
                return;
            }
            userMapper.updateStatus(userId, 0);
            permissionService.evictUserCache(userId);
            notificationService.send(userId, Constants.NOTIFY_UNFROZEN,
                    "您的借阅权限已恢复",
                    "罚款已全部缴清，您的借阅权限已自动恢复，可正常借阅图书。");
            log.info("[账户] 用户罚款缴清自动解冻 userId={}", userId);
        } catch (Exception e) {
            log.warn("[账户] 自动解冻检查失败 userId={}: {}", userId, e.getMessage());
        }
    }

    // ==================== 支付宝沙箱 支付流程 ====================

    /** 创建支付前校验：罚款缴纳 */
    public Result prepareFinePay(Long userId, Long fineId) {
        FineRecord fine = fineRecordMapper.selectById(fineId);
        if (fine == null) return Result.fail("罚款记录不存在");
        if (!fine.getUserId().equals(userId)) return Result.fail("无权操作他人的罚款记录");
        if (fine.getStatus() != Constants.FINE_UNPAID) return Result.fail("该罚款已缴纳");
        return Result.ok(fine.getAmount());
    }

    /** 创建支付前校验：押金充值 */
    public Result prepareDepositPay(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return Result.fail("充值金额必须大于0");
        if (amount.compareTo(MAX_PAY_AMOUNT) > 0) return Result.fail("单次充值上限 " + MAX_PAY_AMOUNT.stripTrailingZeros() + " 元");
        return Result.ok("校验通过");
    }

    /** 支付宝回调确认：罚款核销（不再从押金扣款，因已通过支付宝支付） */
    @Transactional
    public Result confirmFinePaid(Long userId, Long fineId, String outTradeNo, String tradeNo) {
        FineRecord fine = fineRecordMapper.selectById(fineId);
        if (fine == null) return Result.fail("罚款记录不存在");
        if (!fine.getUserId().equals(userId)) return Result.fail("无权操作");
        if (fine.getStatus() != Constants.FINE_UNPAID) {
            return Result.ok("该罚款已缴纳");
        }
        if (fineRecordMapper.pay(fineId, userId) == 0) {
            return Result.fail("罚款状态已变更，请刷新");
        }
        DepositRecord record = new DepositRecord();
        record.setUserId(userId);
        record.setAmount(fine.getAmount());
        record.setType(Constants.DEPOSIT_DEDUCT);
        String remarkText = "支付宝缴纳罚款：订单号" + outTradeNo
                + "（图书：" + (fine.getBookName() == null ? "未知" : "《" + fine.getBookName() + "》") + "）";
        record.setRemark(remarkText.length() > 180 ? remarkText.substring(0, 180) : remarkText);
        depositRecordMapper.insert(record);
        permissionService.evictUserCache(userId);
        tryUnfreeze(userId);
        return Result.ok("罚款缴纳成功，已解除借阅限制");
    }

    /** 支付宝回调确认：押金加款 */
    @Transactional
    public Result confirmDepositPaid(Long userId, BigDecimal amount, String outTradeNo, String tradeNo) {
        // 幂等保护：同步 returnUrl 与异步 notify 双回调只入账一次
        if (!cacheService.setnx(PAY_DEP_LOCK_PREFIX + outTradeNo,
                UUID.randomUUID().toString(), PAY_DEP_LOCK_TTL)) {
            return Result.ok("该订单已处理，请勿重复操作");
        }
        userMapper.updateDeposit(userId, amount);
        DepositRecord record = new DepositRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType(Constants.DEPOSIT_PAY);
        record.setRemark("支付宝押金充值：订单号" + outTradeNo);
        depositRecordMapper.insert(record);
        permissionService.evictUserCache(userId);
        User fresh = userMapper.selectById(userId);
        BigDecimal bal = fresh == null || fresh.getDeposit() == null ? BigDecimal.ZERO : fresh.getDeposit();
        return Result.ok("押金充值成功，当前余额 " + bal.stripTrailingZeros().toPlainString() + " 元");
    }
}