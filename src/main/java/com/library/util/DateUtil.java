package com.library.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期工具类（借阅期限核算 / 超期天数计算）
 */
public final class DateUtil {

    private DateUtil() {
    }

    /** 一天的毫秒数 */
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    /**
     * 在指定时间基础上增加天数
     *
     * @param date 基准时间
     * @param days 增加天数（可为负）
     * @return 新时间
     */
    public static Date addDays(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    /**
     * 计算 later 与 earlier 的间隔天数（不足一天按一天计，向上取整，至少为1）
     * 用于超期罚款计算：超期任意时长即按 1 天起算
     *
     * @param later   较晚时间（如归还时间）
     * @param earlier 较早时间（如应还时间）
     * @return 超期天数
     */
    public static int diffDaysCeil(Date later, Date earlier) {
        if (later == null || earlier == null) {
            return 0;
        }
        long diff = later.getTime() - earlier.getTime();
        if (diff <= 0) {
            return 0;
        }
        int days = (int) Math.ceil(diff * 1.0 / ONE_DAY_MILLIS);
        return Math.max(days, 1);
    }

    /**
     * 计算两个时间相差的整数天（向下取整），用于推荐算法的时间衰减
     */
    public static int diffDays(Date a, Date b) {
        return (int) Math.abs((a.getTime() - b.getTime()) / ONE_DAY_MILLIS);
    }

    /**
     * 格式化为 yyyy-MM-dd
     */
    public static String fmtDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * 格式化为 yyyy-MM-dd HH:mm
     */
    public static String fmtDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}