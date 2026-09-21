package com.library.controller;

import com.library.common.Result;
import com.library.entity.User;
import com.library.service.DepositRefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/admin/refund")
public class AdminDepositRefundController {

    @Autowired
    private DepositRefundService depositRefundService;

    @GetMapping("/list")
    public Result list(@RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.ok(depositRefundService.pageAdmin(status, pageNum, pageSize));
    }

    @PostMapping("/handle")
    public Result handle(@RequestBody Map<String, Object> param, HttpSession session) {
        User admin = (User) session.getAttribute(UserController.SESSION_USER);
        Long id = param.get("id") == null ? null : ((Number) param.get("id")).longValue();
        Integer status = param.get("status") == null ? null : ((Number) param.get("status")).intValue();
        String remark = (String) param.get("remark");
        return depositRefundService.adminHandle(id, status, remark, admin.getId());
    }
}
