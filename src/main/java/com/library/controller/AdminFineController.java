package com.library.controller;

import com.library.common.Result;
import com.library.controller.UserController;
import com.library.entity.User;
import com.library.service.AccountService;
import com.library.dao.FineRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 管理端-罚款对账接口
 */
@RestController
@RequestMapping("/admin/fine")
public class AdminFineController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    /** 罚款对账分页查询（0未缴 1已缴） */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.ok(accountService.pageFines(status, pageNum, pageSize));
    }

    /** 人工核销（线下收款后标记已缴） */
    @PostMapping("/markPaid")
    public Result markPaid(@RequestBody Map<String, Long> param, HttpSession session) {
        User admin = (User) session.getAttribute(UserController.SESSION_USER);
        return accountService.adminMarkPaid(param.get("id"), admin.getId());
    }

    /** 批量人工核销（线下收款后标记已缴） */
    @PostMapping("/batchMarkPaid")
    public Result batchMarkPaid(@RequestBody Map<String, List<Long>> param, HttpSession session) {
        User admin = (User) session.getAttribute(UserController.SESSION_USER);
        List<Long> ids = param.get("ids");
        if (ids == null || ids.isEmpty()) return Result.fail("请选择要核销的罚款记录");
        int rows = fineRecordMapper.batchMarkPaid(ids, admin.getId());
        return Result.ok("已批量核销 " + rows + " 条罚款记录");
    }
}