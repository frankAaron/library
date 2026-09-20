package com.library.controller;

import com.library.common.Result;
import com.library.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-业务统计接口
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private StatsService statsService;

    /** 总览统计（馆藏/读者/在借/超期/未缴罚款/分类分布/热门榜） */
    @GetMapping("/overview")
    public Result overview() {
        return Result.ok(statsService.overview());
    }
}
