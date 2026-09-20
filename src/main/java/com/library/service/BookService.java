package com.library.service;

import com.library.common.Result;
import com.library.entity.Book;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 图书业务接口（查询/管理/封面/热榜）
 */
public interface BookService {

    /** 多条件分页查询（关键词/分类/出版社/作者） */
    Map<String, Object> page(String keyword, Long categoryId, String publisher, String author,
                             Integer pageNum, Integer pageSize);

    /** 图书详情 */
    Book detail(Long id);

    /** 新增图书 */
    Result save(Book book);

    /** 修改图书 */
    Result update(Book book);

    /** 删除图书（存在未归还记录时禁止删除） */
    Result delete(Long id);

    /** 封面上传，返回可访问的相对路径 */
    String uploadCover(MultipartFile file, HttpServletRequest request);

    /** 热门图书TOP10（Redis ZSet 榜单缓存） */
    List<Book> hotTop10();

    /** 图书推荐TOP N */
    List<Book> selectNew(int limit);

    /** 失效热门榜单缓存（借阅行为发生后调用） */
    void evictHot();

    /** 失效首页图书推荐缓存 */
    void evictHome();
}