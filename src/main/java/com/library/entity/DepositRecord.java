package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 押金记录实体
 */
public class DepositRecord {

    /** 记录ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 金额 */
    private BigDecimal amount;
    /** 类型：1缴纳 2扣罚 3退还 */
    private Integer type;
    /** 备注 */
    private String remark;
    /** 操作时间 */
    private Date createTime;

    public DepositRecord() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
