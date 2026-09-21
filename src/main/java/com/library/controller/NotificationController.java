package com.library.controller;

import com.library.common.Result;
import com.library.service.NotificationService;
import com.library.entity.User;
import com.library.controller.UserController;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public Result list(HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        Map<String, Object> data = new HashMap<>();
        data.put("list", notificationService.myNotifications(user.getId()));
        data.put("unread", notificationService.countUnread(user.getId()));
        return Result.ok(data);
    }

    @PostMapping("/markRead")
    public Result markRead(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return notificationService.markRead(param.get("id"), user.getId());
    }

    @PostMapping("/markAllRead")
    public Result markAllRead(HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return notificationService.markAllRead(user.getId());
    }
}
