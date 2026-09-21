package com.library.service;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.DepositRecordMapper;
import com.library.dao.DepositRefundMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.UserMapper;
import com.library.entity.DepositRecord;
import com.library.entity.DepositRefund;
import com.library.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepositRefundService {

    @Autowired
    private DepositRefundMapper depositRefundMapper;
    @Autowired
    private DepositRecordMapper depositRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FineRecordMapper fineRecordMapper;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private NotificationService notificationService;

    public List<DepositRefund> myRefunds(Long userId) {
        return depositRefundMapper.selectByUserId(userId);
    }

    @Transactional
    public Result apply(User sessionUser, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("退款金额必须大于0");
        }
        User u = permissionService.getUserCached(sessionUser.getId());
        BigDecimal balance = u.getDeposit() == null ? BigDecimal.ZERO : u.getDeposit();
        if (amount.compareTo(balance) > 0) {
            return Result.fail("退款金额不能超过当前押金余额（" + balance.stripTrailingZeros().toPlainString() + "元）");
        }
        BigDecimal unpaid = fineRecordMapper.sumUnpaidByUser(sessionUser.getId());
        if (unpaid == null) unpaid = BigDecimal.ZERO;
        if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxRefund = balance.subtract(unpaid);
            if (amount.compareTo(maxRefund) > 0) {
                return Result.fail("您有未缴罚款 " + unpaid.stripTrailingZeros().toPlainString()
                        + " 元，当前最多可退 " + maxRefund.stripTrailingZeros().toPlainString() + " 元");
            }
        }
        DepositRefund r = new DepositRefund();
        r.setUserId(sessionUser.getId());
        r.setAmount(amount);
        r.setReason(reason == null ? "" : reason.length() > 200 ? reason.substring(0, 200) : reason);
        depositRefundMapper.insert(r);
        return Result.ok("退款申请已提交，请等待管理员审核");
    }

    @Transactional
    public Result adminHandle(Long refundId, Integer status, String adminRemark, Long adminId) {
        DepositRefund r = depositRefundMapper.selectById(refundId);
        if (r == null) return Result.fail("退款申请不存在");
        if (r.getStatus() != Constants.REFUND_PENDING) return Result.fail("该申请已处理，请勿重复操作");
        String remark = adminRemark == null ? "" : adminRemark.length() > 500 ? adminRemark.substring(0, 500) : adminRemark;
        if (status == Constants.REFUND_APPROVED) {
            int rows = userMapper.updateDeposit(r.getUserId(), r.getAmount().negate());
            if (rows == 0) return Result.fail("押金扣减失败，请检查用户状态");
            DepositRecord dr = new DepositRecord();
            dr.setUserId(r.getUserId());
            dr.setAmount(r.getAmount().negate());
            dr.setType(Constants.DEPOSIT_REFUND);
            dr.setRemark("管理员审核通过押金退款（申请ID:" + refundId + "）");
            depositRecordMapper.insert(dr);
        }
        depositRefundMapper.updateStatus(refundId, status, remark, adminId);
        permissionService.evictUserCache(r.getUserId());
        try {
            notificationService.send(r.getUserId(), Constants.NOTIFY_REFUND_RESULT,
                    "押金退款申请" + (status == Constants.REFUND_APPROVED ? "已通过" : "已拒绝"),
                    status == Constants.REFUND_APPROVED
                            ? "您申请的 " + r.getAmount().stripTrailingZeros().toPlainString() + " 元押金退款已审核通过，已退回原账户。"
                            : "您申请的 " + r.getAmount().stripTrailingZeros().toPlainString() + " 元押金退款已被管理员拒绝。"
                            + (remark.isEmpty() ? "" : " 原因：" + remark));
        } catch (Exception ignored) {}
        return Result.ok(status == Constants.REFUND_APPROVED ? "已审核通过并完成退款" : "已拒绝该退款申请");
    }

    public Map<String, Object> pageAdmin(Integer status, Integer pageNum, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int offset = (num - 1) * size;
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("offset", offset);
        params.put("pageSize", size);
        List<DepositRefund> list = depositRefundMapper.selectPageAdmin(params);
        long total = depositRefundMapper.countAdmin(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        result.put("pageNum", num);
        result.put("pageSize", size);
        return result;
    }
}
