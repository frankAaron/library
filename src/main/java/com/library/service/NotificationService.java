package com.library.service;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.NotificationMapper;
import com.library.dao.UserMapper;
import com.library.entity.Notification;
import com.library.entity.User;
import com.library.mail.QqMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private QqMailService qqMailService;

    public void send(Long userId, int type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        notificationMapper.insert(n);
        User u = userMapper.selectById(userId);
        if (u != null && u.getEmail() != null && !u.getEmail().isEmpty()) {
            qqMailService.sendHtml(u.getEmail(), "[智慧图书馆]" + title,
                    "<div style=\"font-family:'Microsoft YaHei',sans-serif;max-width:560px;margin:20px auto;\">"
                            + "<h2 style=\"color:#2c3e50;border-bottom:2px solid #3498db;padding-bottom:8px;\">"
                            + title + "</h2>"
                            + "<div style=\"color:#555;line-height:1.8;font-size:15px;padding:12px 0;\">"
                            + content.replace("\n", "<br>")
                            + "</div>"
                            + "<p style=\"color:#999;font-size:12px;border-top:1px solid #eee;padding-top:10px;\">"
                            + "此邮件由智慧图书馆系统自动发送，请勿直接回复。</p></div>");
        }
    }

    public List<Notification> myNotifications(Long userId) {
        return notificationMapper.selectByUserId(userId);
    }

    public long countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public Result markRead(Long id, Long userId) {
        if (notificationMapper.markRead(id, userId) > 0) {
            return Result.ok("已标记为已读");
        }
        return Result.fail("消息不存在或无权操作");
    }

    public Result markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
        return Result.ok("全部标记已读");
    }
}
