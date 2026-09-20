package com.library.controller;

import com.library.common.Result;
import com.library.controller.UserController;
import com.library.entity.User;
import com.library.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * 个性化推荐接口
 */
@RestController
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/reco/list")
    public Result list(HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return Result.ok(recommendService.recommend(user));
    }
}