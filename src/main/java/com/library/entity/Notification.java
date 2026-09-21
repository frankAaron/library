package com.library.entity;

import java.util.Date;

public class Notification {
    private Long id;
    private Long userId;
    private Integer type;
    private String title;
    private String content;
    private boolean read;
    private Date createTime;

    public Notification() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
