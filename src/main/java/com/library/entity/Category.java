package com.library.entity;

/**
 * 图书分类实体
 */
public class Category {

    /** 分类ID */
    private Long id;
    /** 分类名称 */
    private String categoryName;
    /** 分类编码 */
    private String categoryCode;
    /** 分类描述 */
    private String description;
    /** 关联字段：该分类下图书数量 */
    private Integer bookCount;

    public Category() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getBookCount() { return bookCount; }
    public void setBookCount(Integer bookCount) { this.bookCount = bookCount; }
}
