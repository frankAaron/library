package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户实体（含三级身份差异化权限参数）
 */
public class User {

    /** 用户ID */
    private Long id;
    /** 登录账号 */
    private String username;
    /** 密码（MD5） */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 角色：0管理员 1学生 2教师 3访客 */
    private Integer role;
    /** 学号/工号/访客登记号 */
    private String stuOrJobNo;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 借阅额度（本） */
    private Integer maxBorrowCount;
    /** 借阅时长（天） */
    private Integer maxBorrowDays;
    /** 可续借次数 */
    private Integer maxRenewCount;
    /** 超期日罚款标准（元/天/本） */
    private BigDecimal finePerDay;
    /** 押金余额（元） */
    private BigDecimal deposit;
    /** 状态：0正常 1停用 */
    private Integer status;
    /** 注册时间 */
    private Date createTime;

    public User() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getStuOrJobNo() { return stuOrJobNo; }
    public void setStuOrJobNo(String stuOrJobNo) { this.stuOrJobNo = stuOrJobNo; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getMaxBorrowCount() { return maxBorrowCount; }
    public void setMaxBorrowCount(Integer maxBorrowCount) { this.maxBorrowCount = maxBorrowCount; }
    public Integer getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(Integer maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }
    public Integer getMaxRenewCount() { return maxRenewCount; }
    public void setMaxRenewCount(Integer maxRenewCount) { this.maxRenewCount = maxRenewCount; }
    public BigDecimal getFinePerDay() { return finePerDay; }
    public void setFinePerDay(BigDecimal finePerDay) { this.finePerDay = finePerDay; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
