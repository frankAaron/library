package com.library.entity;

import java.util.Date;

/**
 * 浏览历史实体（个性化推荐数据源）
 */
public class BrowseHistory {

    /** 记录ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 图书ID */
    private Long bookId;
    /** 浏览时间 */
    private Date browseTime;

    /* ==================== 关联查询字段 ==================== */
    /** 图书名称 */
    private String bookName;
    /** 图书分类ID */
    private Long categoryId;

    public BrowseHistory() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Date getBrowseTime() { return browseTime; }
    public void setBrowseTime(Date browseTime) { this.browseTime = browseTime; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
