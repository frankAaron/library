package com.library.service;

import com.library.common.Result;
import com.library.entity.Category;

import java.util.List;

/**
 * 图书分类业务接口
 */
public interface CategoryService {

    /** 全部分类（Redis 缓存 15 分钟） */
    List<Category> listAll();

    /** 按ID查询 */
    Category selectById(Long id);

    /** 新增分类 */
    Result save(Category category);

    /** 修改分类 */
    Result update(Category category);

    /** 删除分类（分类下存在图书时禁止删除） */
    Result delete(Long id);
}
