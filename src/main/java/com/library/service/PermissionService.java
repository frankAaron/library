package com.library.service;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.UserMapper;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限服务（三级身份精细化权限管控核心）
 * <p>
 * 1. 用户权限信息 Redis 缓存（2小时），降低高频权限校验的数据库压力；
 * 2. 借阅前置校验：账号状态 → 未缴罚款 → 押金门槛 → 重复借阅 → 借阅额度 → 库存；
 * 3. 续借前置校验：状态 → 超期 → 剩余续借次数。
 */
@Service
public class PermissionService {

    /** 三级身份押金门槛（元）：学生50 教师0 访客100 */
    private static final Map<Integer, BigDecimal> DEPOSIT_REQUIRED = new HashMap<>();

    static {
        DEPOSIT_REQUIRED.put(Constants.ROLE_STUDENT, new BigDecimal("50"));
        DEPOSIT_REQUIRED.put(Constants.ROLE_TEACHER, BigDecimal.ZERO);
        DEPOSIT_REQUIRED.put(Constants.ROLE_VISITOR, new BigDecimal("100"));
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    @Autowired
    private CacheService cacheService;

    /**
     * 查询用户信息（带 Redis 缓存，缓存值不包含密码）
     * 用于高频的权限校验与借阅额度判断
     */
    public User getUserCached(Long userId) {
        return cacheService.getOrLoad(Constants.KEY_USER_PERM + userId, 7200, User.class, () -> {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setPassword(null);
                fillDefaults(user);
            }
            return user;
        });
    }

    private void fillDefaults(User user) {
        if (user.getDeposit() == null) {
            user.setDeposit(BigDecimal.ZERO);
        }
        if (user.getFinePerDay() == null) {
            user.setFinePerDay(new BigDecimal("0.5"));
        }
        if (user.getMaxBorrowCount() == null) {
            user.setMaxBorrowCount(5);
        }
        if (user.getMaxBorrowDays() == null) {
            user.setMaxBorrowDays(30);
        }
        if (user.getMaxRenewCount() == null) {
            user.setMaxRenewCount(1);
        }
    }

    /** 用户权限/押金变更后主动失效缓存 */
    public void evictUserCache(Long userId) {
        cacheService.evict(Constants.KEY_USER_PERM + userId);
    }

    /** 按角色批量失效用户权限缓存（管理员批量调整权限后调用） */
    public void evictUserCacheByRole(Integer role) {
        cacheService.evictPattern(Constants.KEY_USER_PERM + "*");
    }

    /** 获取身份对应的押金门槛 */
    public BigDecimal getDepositRequired(Integer role) {
        return DEPOSIT_REQUIRED.getOrDefault(role, BigDecimal.ZERO);
    }

    /**
     * 借阅前置校验（业务闭环第一步：借阅校验）
     * 校验顺序：账号状态 → 未缴罚款 → 押金门槛 → 重复借阅 → 借阅额度 → 库存
     *
     * @param user 借阅人（最新权限信息）
     * @param book 待借图书
     * @return 校验结果，code=200 表示通过
     */
    public Result checkBorrow(User user, Book book) {
        if (user == null) {
            return Result.fail("用户信息不存在");
        }
        // 1. 账号状态校验
        if (user.getStatus() != null && user.getStatus() != 0) {
            return Result.fail("账号已被停用，请联系管理员");
        }
        // 2. 未缴罚款限制（超期未缴费则冻结借阅权限）
        if (fineRecordMapper.countUnpaidByUser(user.getId()) > 0) {
            return Result.fail("您有未缴纳的超期罚款，请先在「我的借阅」中缴纳罚款后再借阅");
        }
        // 3. 押金门槛校验（差异化身份要求）
        BigDecimal required = getDepositRequired(user.getRole());
        BigDecimal deposit = user.getDeposit() == null ? BigDecimal.ZERO : user.getDeposit();
        if (deposit.compareTo(required) < 0) {
            return Result.fail(String.format("押金不足（当前身份需缴押金%s元，当前余额%s元），请先在个人中心充值押金",
                    required.stripTrailingZeros().toPlainString(), deposit.stripTrailingZeros().toPlainString()));
        }
        // 4. 同一图书不可重复借阅（未归还前）
        BorrowRecord borrowing = borrowRecordMapper.selectBorrowing(user.getId(), book.getId());
        if (borrowing != null) {
            return Result.fail("您已借阅《" + book.getBookName() + "》且尚未归还，不能重复借阅");
        }
        // 5. 借阅额度校验（差异化额度）
        long borrowingCount = borrowRecordMapper.countBorrowing(user.getId());
        if (borrowingCount >= user.getMaxBorrowCount()) {
            return Result.fail("已达到当前身份的借阅额度上限（" + user.getMaxBorrowCount()
                    + "本），请先归还部分图书");
        }
        // 6. 库存校验
        if (book.getStock() == null || book.getStock() <= 0) {
            return Result.fail("库存不足，您可以预订此书");
        }
        return Result.ok();
    }

    /**
     * 预约前置校验
     * 校验顺序：账号状态 → 未缴罚款
     */
    public Result checkReserve(User user) {
        if (user == null) {
            return Result.fail("用户信息不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return Result.fail("账号已被停用，请联系管理员");
        }
        if (fineRecordMapper.countUnpaidByUser(user.getId()) > 0) {
            return Result.fail("您有未缴纳的超期罚款，请先在「我的借阅」中缴纳罚款后再预约");
        }
        return Result.ok();
    }

    /**
     * 续借前置校验
     * 校验顺序：记录状态 → 是否超期 → 身份是否支持续借 → 剩余续借次数
     */
    public Result checkRenew(User user, BorrowRecord record) {
        // 1. 仅"借阅中"状态可续借（已归还/超期不可续借）
        if (record.getStatus() == null || record.getStatus() != Constants.RECORD_BORROWING) {
            return Result.fail("仅借阅中的图书可以续借");
        }
        // 2. 已过应还日期不可续借（含未跑定时任务但实际已超期的情况）
        if (record.getDueDate() != null && record.getDueDate().before(new Date())) {
            return Result.fail("图书已超期，不可续借，请先归还并缴纳罚款");
        }
        // 3. 身份续借规则（访客不支持续借）
        if (user.getMaxRenewCount() == null || user.getMaxRenewCount() <= 0) {
            return Result.fail("当前身份不支持续借，请按期归还");
        }
        // 4. 续借次数校验
        int renewed = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (renewed >= user.getMaxRenewCount()) {
            return Result.fail("续借次数已用完（当前身份最多可续借" + user.getMaxRenewCount() + "次）");
        }
        return Result.ok();
    }
}