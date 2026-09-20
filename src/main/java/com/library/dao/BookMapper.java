package com.library.dao;

import com.library.entity.Book;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 图书 DAO
 */
public interface BookMapper {

    /** 新增图书 */
    int insert(Book book);

    /** 修改图书 */
    int update(Book book);

    /** 删除图书 */
    int deleteById(@Param("id") Long id);

    /** 按ID查询 */
    Book selectById(@Param("id") Long id);

    /**
     * 多条件分页查询（关键词模糊匹配书名/作者/ISBN，支持分类/出版社/作者精确筛选）
     */
    List<Book> selectPage(@Param("keyword") String keyword,
                          @Param("categoryId") Long categoryId,
                          @Param("publisher") String publisher,
                          @Param("author") String author,
                          @Param("offset") int offset,
                          @Param("size") int size);

    /** 查询总数 */
    long countPage(@Param("keyword") String keyword,
                   @Param("categoryId") Long categoryId,
                   @Param("publisher") String publisher,
                   @Param("author") String author);

    /** 扣减库存（条件更新：stock>0 才扣减，防止并发超卖） */
    int deductStock(@Param("id") Long id);

    /** 归还回补库存 */
    int restoreStock(@Param("id") Long id);

    /** 累计借阅次数+1（热度指标） */
    int incrBorrowCount(@Param("id") Long id);

    /** 热门图书TOP N（按累计借阅量倒序） */
    List<Book> selectHot(@Param("limit") int limit);

    /** 图书推荐TOP N */
    List<Book> selectNew(@Param("limit") int limit);

    /** 按ID集合批量查询 */
    List<Book> selectByIds(@Param("ids") List<Long> ids);

    /** 推荐候选集：指定分类下有库存且排除已借阅图书 */
    List<Book> selectByCategories(@Param("categoryIds") List<Long> categoryIds,
                                  @Param("excludeBookIds") List<Long> excludeBookIds,
                                  @Param("limit") int limit);

    /** 统计该书借阅中/超期的记录数（删除图书前校验） */
    long countBorrowingByBook(@Param("bookId") Long bookId);

    /** 馆藏总册数（统计页） */
    BigDecimal sumTotalCount();

    /** 图书总数（统计页） */
    long countAll();
}
