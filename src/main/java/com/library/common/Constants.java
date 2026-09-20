package com.library.common;

/**
 * 系统常量定义
 */
public final class Constants {

    private Constants() {
    }

    /* ==================== 用户角色 ==================== */
    /** 管理员 */
    public static final int ROLE_ADMIN = 0;
    /** 学生 */
    public static final int ROLE_STUDENT = 1;
    /** 教师 */
    public static final int ROLE_TEACHER = 2;
    /** 访客 */
    public static final int ROLE_VISITOR = 3;

    /** 角色名称（页面展示用） */
    public static final String[] ROLE_NAMES = {"管理员", "学生", "教师", "访客"};

    /* ==================== 借阅记录状态 ==================== */
    /** 借阅中 */
    public static final int RECORD_BORROWING = 0;
    /** 已归还 */
    public static final int RECORD_RETURNED = 1;
    /** 超期（借阅中且已过应还日期） */
    public static final int RECORD_OVERDUE = 2;
    /** 超期已缴费归还 */
    public static final int RECORD_OVERDUE_PAID = 3;

    /* ==================== 预订状态 ==================== */
    public static final int RESERVE_WAITING = 0;
    public static final int RESERVE_NOTIFIED = 1;
    public static final int RESERVE_FINISHED = 2;
    public static final int RESERVE_CANCELED = 3;

    /* ==================== 罚款状态 ==================== */
    public static final int FINE_UNPAID = 0;
    public static final int FINE_PAID = 1;

    /* ==================== 押金记录类型 ==================== */
    public static final int DEPOSIT_PAY = 1;
    public static final int DEPOSIT_DEDUCT = 2;
    public static final int DEPOSIT_REFUND = 3;

    /* ==================== 到期预警提前天数 ==================== */
    public static final int DUE_WARN_DAYS = 3;

    /* ==================== Redis 缓存 Key 常量 ==================== */
    /** 热门图书榜单(ZSet) */
    public static final String KEY_HOT_BOOKS = "book:hot:top10";
    /** 用户权限缓存 */
    public static final String KEY_USER_PERM = "user:perm:";
    /** 首页公共数据缓存 */
    public static final String KEY_HOME_PUBLIC = "home:public:data";
    /** 个性化推荐缓存 */
    public static final String KEY_RECO = "reco:";
    /** 分类列表缓存 */
    public static final String KEY_CATEGORY_LIST = "category:list";
}
