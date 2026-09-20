package com.library.dao;

import com.library.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书分类 DAO
 */
public interface CategoryMapper {

    /** 新增分类 */
    int insert(Category category);

    /** 修改分类 */
    int update(Category category);

    /** 删除分类 */
    int deleteById(@Param("id") Long id);

    /** 按ID查询 */
    Category selectById(@Param("id") Long id);

    /** 查询全部分类（携带各分类图书数量） */
    List<Category> selectAll();

    /** 统计分类下图书数量（删除前校验） */
    long countBooks(@Param("categoryId") Long categoryId);
}
