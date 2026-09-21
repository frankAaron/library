package com.library.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class QqMailService {

    private static final Logger log = LoggerFactory.getLogger(QqMailService.class);

    @Value("${mail.host:smtp.qq.com}")
    private String host;
    @Value("${mail.port:465}")
    private String port;
    @Value("${mail.username:}")
    private String username;
    @Value("${mail.password:}")
    private String password;
    @Value("${mail.from:}")
    private String from;
    @Value("${mail.nickname:智慧图书馆}")
    private String nickname;
    @Value("${mail.ssl:true}")
    private boolean ssl;
    @Value("${mail.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled && username != null && !username.isEmpty()
                && password != null && !password.isEmpty()
                && !"PUT_QQ_EMAIL_AUTH_CODE_HERE".equals(password);
    }

    public boolean sendHtml(String to, String subject, String htmlBody) {
        if (!isEnabled()) {
            log.warn("[邮件] 未配置或已禁用，跳过发送 to={}, subject={}", to, subject);
            return false;
        }
        if (to == null || to.isEmpty()) {
            log.warn("[邮件] 收件人为空，跳过发送 subject={}", subject);
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", port);
        }
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, nickname));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject(subject, "UTF-8");
            msg.setContent(htmlBody, "text/html;charset=UTF-8");
            Transport.send(msg);
            log.info("[邮件] 发送成功 to={}, subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("[邮件] 发送失败 to={}, subject={}, 原因={}", to, subject, e.getMessage());
            return false;
        }
    }

    public boolean sendPlain(String to, String subject, String text) {
        return sendHtml(to, subject, "<pre style=\"font-family:inherit;\">" + text + "</pre>");
    }
}
