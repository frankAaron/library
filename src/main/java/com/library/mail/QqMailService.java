package com.library.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * QQ 邮箱发送服务
 * <p>
 * 设计要点：
 * 1. 异步发送：邮件属于旁路通知，提交到独立 daemon 线程池后立即返回，
 *    SMTP 网络抖动/超时不阻塞借阅、归还等主业务事务；
 * 2. 有界队列 + 拒绝丢弃：队列满时丢弃新邮件并告警，绝不拖垮主业务；
 * 3. SMTP 三类超时（连接/读/写各 8 秒），防止服务器不可达时线程永久挂死；
 * 4. Session 构建一次复用（Jakarta Mail Session 线程安全）。
 */
@Component
public class QqMailService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(QqMailService.class);

    /** SMTP 连接/读取/写入超时（毫秒） */
    private static final String TIMEOUT_MS = "8000";
    /** 待发邮件队列上限：满后丢弃新任务（旁路通知不得影响主业务） */
    private static final int QUEUE_CAPACITY = 200;
    /** 容器关闭时等待存量邮件发送的最长时间（秒） */
    private static final long SHUTDOWN_WAIT_SECONDS = 10;

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

    /** Jakarta Mail Session 线程安全，配置不变只构建一次 */
    private volatile Session session;

    /** 异步发送线程池：2 个常驻 daemon 线程 + 有界队列 + 满则丢弃 */
    private final ExecutorService mailExecutor = new ThreadPoolExecutor(
            2, 2, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread t = new Thread(r, "qq-mail-sender");
                t.setDaemon(true);
                return t;
            },
            (r, executor) ->
                    log.warn("[邮件] 发送队列已满（{}），丢弃一封通知邮件", QUEUE_CAPACITY));

    public boolean isEnabled() {
        return enabled
                && !isBlank(username)
                && !isBlank(password)
                && !isBlank(from)
                && !"PUT_QQ_EMAIL_AUTH_CODE_HERE".equals(password);
    }

    /**
     * 异步发送 HTML 邮件：配置/地址校验通过后提交后台线程立即返回，
     * 任何 SMTP 失败仅记录日志，不影响调用方业务。
     *
     * @return true=已提交发送队列；false=未发送（未启用 / 收件人非法 / 提交失败）
     */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        if (!isEnabled()) {
            log.warn("[邮件] 未配置或已禁用，跳过发送 to={}, subject={}", to, subject);
            return false;
        }
        if (isBlank(to)) {
            log.warn("[邮件] 收件人为空，跳过发送 subject={}", subject);
            return false;
        }
        try {
            new InternetAddress(to).validate();
        } catch (AddressException e) {
            log.warn("[邮件] 收件人地址非法 to={}: {}", to, e.getMessage());
            return false;
        }
        try {
            mailExecutor.execute(() -> doSendHtml(to, subject, htmlBody));
        } catch (Exception e) {
            // 兜底：有界队列已配拒绝策略，正常不会抛到这里，防止极端情况影响主流程
            log.warn("[邮件] 提交发送任务失败 to={}: {}", to, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean sendPlain(String to, String subject, String text) {
        return sendHtml(to, subject, "<pre style=\"font-family:inherit;\">" + text + "</pre>");
    }

    /** 实际执行 SMTP 发送（仅在邮件线程池中调用），异常全部吞掉只记日志 */
    private void doSendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(from, nickname, "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject(subject, "UTF-8");
            msg.setContent(htmlBody, "text/html;charset=UTF-8");
            Transport.send(msg);
            log.info("[邮件] 发送成功 to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[邮件] 发送失败 to={}, subject={}, 原因={}", to, subject, e.getMessage());
        }
    }

    /** 双重检查锁构建 Mail Session（Session 线程安全，全局复用一个） */
    private Session getSession() {
        Session s = session;
        if (s == null) {
            synchronized (this) {
                s = session;
                if (s == null) {
                    Properties props = new Properties();
                    props.put("mail.smtp.host", host);
                    props.put("mail.smtp.port", port);
                    props.put("mail.smtp.auth", "true");
                    // 三类超时，避免 SMTP 异常时长时间阻塞发送线程
                    props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
                    props.put("mail.smtp.timeout", TIMEOUT_MS);
                    props.put("mail.smtp.writetimeout", TIMEOUT_MS);
                    if (ssl) {
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                        props.put("mail.smtp.socketFactory.port", port);
                    }
                    final String authUser = username;
                    final String authPwd = password;
                    s = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(authUser, authPwd);
                        }
                    });
                    session = s;
                }
            }
        }
        return s;
    }

    /** 容器关闭时优雅停止线程池：最多等待 10 秒发完存量邮件，再强制关闭 */
    @Override
    public void destroy() {
        mailExecutor.shutdown();
        try {
            if (!mailExecutor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                mailExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mailExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
