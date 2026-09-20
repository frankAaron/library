package com.library.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 图书实体
 */
public class Book {

    /** 图书ID */
    private Long id;
    /** 书名 */
    private String bookName;
    /** 作者 */
    private String author;
    /** 出版社 */
    private String publisher;
    /** ISBN */
    private String isbn;
    /** 分类ID */
    private Long categoryId;
    /** 封面图片路径 */
    private String coverUrl;
    /** 定价 */
    private BigDecimal price;
    /** 当前可借库存 */
    private Integer stock;
    /** 馆藏总数 */
    private Integer totalCount;
    /** 累计借阅次数（热度指标） */
    private Integer borrowCount;
    /** 馆藏位置 */
    private String location;
    /** 内容简介 */
    private String description;
    /** 入库时间 */
    private Date createTime;
    /** 关联字段：分类名称 */
    private String categoryName;

    public Book() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getBorrowCount() { return borrowCount; }
    public void setBorrowCount(Integer borrowCount) { this.borrowCount = borrowCount; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
