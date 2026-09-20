package com.library.entity;

import java.util.Date;

/**
 * 图书预订实体
 */
public class Reservation {

    /** 预订ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 图书ID */
    private Long bookId;
    /** 预订时间 */
    private Date reserveTime;
    /** 状态：0排队中 1已通知 2已完成 3已取消 */
    private Integer status;
    /** 到书通知时间 */
    private Date notifyTime;

    /* ==================== 关联查询字段 ==================== */
    /** 图书名称 */
    private String bookName;
    /** 图书作者 */
    private String author;
    /** 预订人姓名 */
    private String realName;

    public Reservation() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Date getReserveTime() { return reserveTime; }
    public void setReserveTime(Date reserveTime) { this.reserveTime = reserveTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getNotifyTime() { return notifyTime; }
    public void setNotifyTime(Date notifyTime) { this.notifyTime = notifyTime; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
}
