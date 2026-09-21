package com.library.task;

import com.library.common.Constants;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.UserMapper;
import com.library.entity.User;
import com.library.service.CacheService;
import com.library.service.NotificationService;
import com.library.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class BorrowTask {

    private static final Logger log = LoggerFactory.getLogger(BorrowTask.class);

    private static final String LOCK_PREFIX = "library:task:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 300;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private FineRecordMapper fineRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 30 0 * * ?")
    public void markOverdueDaily() {
        String lockKey = LOCK_PREFIX + "markOverdue";
        String token = UUID.randomUUID().toString();
        if (!cacheService.tryLock(lockKey, token, LOCK_EXPIRE_SECONDS)) {
            return;
        }
        try {
            int count = borrowRecordMapper.markOverdue();
            log.info("[定时任务] 超期检查完成，本次标记超期记录 {} 条", count);
        } finally {
            cacheService.releaseLock(lockKey, token);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void dueSoonWarn() {
        String lockKey = LOCK_PREFIX + "dueSoonWarn";
        String token = UUID.randomUUID().toString();
        if (!cacheService.tryLock(lockKey, token, LOCK_EXPIRE_SECONDS)) {
            return;
        }
        try {
            Map<Long, Long> dueSoon = new HashMap<>();
            borrowRecordMapper.countDueSoonByUser(Constants.DUE_WARN_DAYS).forEach(r -> {
                long userId = ((Number) r.get("userId")).longValue();
                long cnt = ((Number) r.get("cnt")).longValue();
                dueSoon.put(userId, cnt);
            });
            if (!dueSoon.isEmpty()) {
                for (Map.Entry<Long, Long> e : dueSoon.entrySet()) {
                    try {
                        notificationService.send(e.getKey(), Constants.NOTIFY_DUE_SOON,
                                "您有图书即将到期",
                                "您借阅的 " + e.getValue() + " 本图书将在 " + Constants.DUE_WARN_DAYS
                                        + " 天内到期，请及时归还或续借，避免产生超期罚款。");
                    } catch (Exception ex) {
                        log.warn("到期提醒消息发送失败 userId={}: {}", e.getKey(), ex.getMessage());
                    }
                }
            }
            log.info("[定时任务] 到期预警：已向 {} 名读者发送到期提醒消息", dueSoon.size());
        } finally {
            cacheService.releaseLock(lockKey, token);
        }
    }

    @Scheduled(cron = "0 10 0 * * ?")
    public void autoFreezeByOverdue() {
        String lockKey = LOCK_PREFIX + "autoFreeze";
        String token = UUID.randomUUID().toString();
        if (!cacheService.tryLock(lockKey, token, LOCK_EXPIRE_SECONDS)) {
            return;
        }
        try {
            int frozenByDays = borrowRecordMapper.freezeByOverdueDays(Constants.OVERDUE_FREEZE_DAYS);
            List<Map<String, Object>> overdueList = borrowRecordMapper.listOverdueUserTotal();
            Map<Long, BigDecimal> overdueUsers = new HashMap<>();
            for (Map<String, Object> r : overdueList) {
                long uid = ((Number) r.get("userId")).longValue();
                BigDecimal overdueTotal = r.get("overdueTotal") == null ? BigDecimal.ZERO
                        : new BigDecimal(r.get("overdueTotal").toString());
                overdueUsers.put(uid, overdueTotal);
            }
            int frozenByFine = 0;
            for (Map.Entry<Long, BigDecimal> e : overdueUsers.entrySet()) {
                BigDecimal unpaid = fineRecordMapper.sumUnpaidByUser(e.getKey());
                BigDecimal total = unpaid == null ? BigDecimal.ZERO : unpaid.add(e.getValue());
                if (total.compareTo(Constants.FINE_FREEZE_THRESHOLD) >= 0) {
                    int rows = userMapper.updateStatus(e.getKey(), 1);
                    if (rows > 0) {
                        frozenByFine++;
                        permissionService.evictUserCache(e.getKey());
                        try {
                            notificationService.send(e.getKey(), Constants.NOTIFY_OVERDUE_FROZEN,
                                    "您的借阅权限已被冻结",
                                    "由于累计超期罚款（" + total.stripTrailingZeros().toPlainString()
                                            + " 元）已达冻结阈值 "
                                            + Constants.FINE_FREEZE_THRESHOLD.stripTrailingZeros().toPlainString()
                                            + " 元，您的借阅权限已被临时冻结。请尽快缴纳罚款后自动解冻。");
                        } catch (Exception ex) {
                            log.warn("冻结通知发送失败 userId={}: {}", e.getKey(), ex.getMessage());
                        }
                    }
                }
            }
            log.info("[定时任务] 超期冻结：超期{}天冻结 {} 人，累计罚款≥{}元冻结 {} 人",
                    Constants.OVERDUE_FREEZE_DAYS, frozenByDays,
                    Constants.FINE_FREEZE_THRESHOLD.stripTrailingZeros().toPlainString(), frozenByFine);
        } finally {
            cacheService.releaseLock(lockKey, token);
        }
    }
}