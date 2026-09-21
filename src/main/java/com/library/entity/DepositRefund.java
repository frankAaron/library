package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

public class DepositRefund {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private String reason;
    private String adminRemark;
    private Date applyTime;
    private Date handleTime;

    public DepositRefund() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public Date getApplyTime() { return applyTime; }
    public void setApplyTime(Date applyTime) { this.applyTime = applyTime; }
    public Date getHandleTime() { return handleTime; }
    public void setHandleTime(Date handleTime) { this.handleTime = handleTime; }
}
