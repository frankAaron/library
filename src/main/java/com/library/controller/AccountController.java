package com.library.controller;

import com.library.common.Result;
import com.library.controller.UserController;
import com.library.entity.User;
import com.library.service.AccountService;
import com.library.service.DepositRefundService;
import com.library.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 账户接口（罚款缴纳 / 押金充值与查询）
 */
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DepositRefundService depositRefundService;

    /** 缴纳超期罚款（从押金余额扣款） */
    @PostMapping("/fine/pay")
    public Result payFine(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return accountService.payFine(user, param.get("fineId"));
    }

    /** 押金充值 */
    @PostMapping("/deposit/pay")
    public Result payDeposit(@RequestBody Map<String, BigDecimal> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return accountService.payDeposit(user, param.get("amount"));
    }

    /** 我的押金信息（余额 + 流水） */
    @GetMapping("/deposit/my")
    public Result myDeposit(HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        User fresh = permissionService.getUserCached(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("balance", fresh == null || fresh.getDeposit() == null ? BigDecimal.ZERO : fresh.getDeposit());
        data.put("records", accountService.myDepositRecords(user.getId()));
        data.put("refunds", depositRefundService.myRefunds(user.getId()));
        return Result.ok(data);
    }

    @PostMapping("/deposit/refund/apply")
    public Result applyRefund(@RequestBody Map<String, Object> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        BigDecimal amount = param.get("amount") == null ? null : new BigDecimal(param.get("amount").toString());
        String reason = (String) param.get("reason");
        return depositRefundService.apply(user, amount, reason);
    }
}