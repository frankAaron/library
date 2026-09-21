package com.library.controller;

import com.library.common.Result;
import com.library.controller.UserController;
import com.library.entity.User;
import com.library.mail.QqMailService;
import com.library.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private QqMailService qqMailService;

    @GetMapping("/notify")
    public Result testNotify(HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        if (user == null) return Result.fail("请先登录");

        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("emailSet", user.getEmail() != null && !user.getEmail().isEmpty());
        info.put("mailEnabled", qqMailService.isEnabled());

        notificationService.send(user.getId(), 99,
                "【测试】消息中心连通性验证",
                "这是一条测试站内消息。如果您同时收到了 QQ 邮件，说明整条链路（数据库 + 邮件）工作正常！"
                        + "\n\n系统时间：" + java.time.LocalDateTime.now());

        info.put("msgCount", notificationService.countUnread(user.getId()));
        return Result.ok(info);
    }
}
