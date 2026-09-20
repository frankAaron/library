package com.library.controller;

import com.library.common.Result;
import com.library.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-借阅记录管理接口
 */
@RestController
@RequestMapping("/admin/borrow")
public class AdminBorrowController {

    @Autowired
    private BorrowService borrowService;

    /** 借阅记录分页查询（关键词/状态/借出日期区间） */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "startDate", required = false) String startDate,
                       @RequestParam(value = "endDate", required = false) String endDate,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.ok(borrowService.pageAdmin(keyword, status, startDate, endDate, pageNum, pageSize));
    }

    /** 手动触发超期检查（正式环境由每日定时任务自动执行） */
    @PostMapping("/markOverdue")
    public Result markOverdue() {
        int count = borrowService.markOverdue();
        return Result.ok("超期检查完成，本次标记超期记录 " + count + " 条");
    }
}