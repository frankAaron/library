package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 罚款记录实体
 */
public class FineRecord {

    /** 罚款ID */
    private Long id;
    /** 关联借阅记录ID */
    private Long borrowRecordId;
    /** 用户ID */
    private Long userId;
    /** 罚款金额 */
    private BigDecimal amount;
    /** 状态：0未缴 1已缴 */
    private Integer status;
    /** 缴纳时间 */
    private Date payTime;
    /** 核销操作管理员ID */
    private Long operatorId;

    /* ==================== 关联查询字段 ==================== */
    /** 图书名称 */
    private String bookName;
    /** 用户姓名 */
    private String realName;
    /** 用户账号 */
    private String username;

    public FineRecord() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBorrowRecordId() { return borrowRecordId; }
    public void setBorrowRecordId(Long borrowRecordId) { this.borrowRecordId = borrowRecordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
