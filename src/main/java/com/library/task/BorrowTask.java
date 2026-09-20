package com.library.task;

import com.library.common.Constants;
import com.library.dao.BorrowRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 借阅业务定时任务（借阅全流程自动化的重要组成）
 * <p>
 * 1. 每日 00:30 超期标记：将已过应还日期的"借阅中"记录批量置为"超期"状态，
 *    作为超期自动计费的触发起点；
 * 2. 每日 08:00 到期预警统计：记录即将到期（3天内）的借阅数量日志，
 *    读者端首页/我的借阅页同时会实时展示到期预警横幅。
 */
@Component
public class BorrowTask {

    private static final Logger log = LoggerFactory.getLogger(BorrowTask.class);

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    /** 每日 00:30 自动执行超期标记 */
    @Scheduled(cron = "0 30 0 * * ?")
    public void markOverdueDaily() {
        int count = borrowRecordMapper.markOverdue();
        log.info("[定时任务] 超期检查完成，本次标记超期记录 {} 条", count);
    }

    /** 每日 08:00 统计到期预警数量 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void dueSoonWarn() {
        long count = borrowRecordMapper.countDueSoonAll(Constants.DUE_WARN_DAYS);
        log.info("[定时任务] 到期预警：{} 本图书将于{}天内到期", count, Constants.DUE_WARN_DAYS);
    }
}
