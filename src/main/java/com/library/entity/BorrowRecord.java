package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 借阅记录实体
 */
public class BorrowRecord {

    /** 记录ID */
    private Long id;
    /** 借阅用户ID */
    private Long userId;
    /** 图书ID */
    private Long bookId;
    /** 借出时间 */
    private Date borrowDate;
    /** 应还时间（系统自动计算） */
    private Date dueDate;
    /** 实际归还时间 */
    private Date returnDate;
    /** 状态：0借阅中 1已归还 2超期 3超期已缴费归还 */
    private Integer status;
    /** 已续借次数 */
    private Integer renewCount;
    /** 累计罚款金额 */
    private BigDecimal fineAmount;

    /* ==================== 关联查询字段 ==================== */
    /** 图书名称 */
    private String bookName;
    /** 作者 */
    private String author;
    /** 图书分类ID（推荐算法联查使用） */
    private Long categoryId;
    /** 借阅人姓名 */
    private String realName;
    /** 借阅人账号 */
    private String username;

    public BorrowRecord() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRenewCount() { return renewCount; }
    public void setRenewCount(Integer renewCount) { this.renewCount = renewCount; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
