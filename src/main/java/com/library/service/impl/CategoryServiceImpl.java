package com.library.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.CategoryMapper;
import com.library.entity.Category;
import com.library.service.CacheService;
import com.library.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图书分类业务实现（列表走 Redis 缓存，增删改后主动失效）
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CacheService cacheService;

    @Override
    public List<Category> listAll() {
        return cacheService.getOrLoad(Constants.KEY_CATEGORY_LIST, 900,
                new TypeReference<List<Category>>() {
                }, categoryMapper::selectAll);
    }

    @Override
    public Category selectById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional
    public Result save(Category category) {
        if (isBlank(category.getCategoryName()) || isBlank(category.getCategoryCode())) {
            return Result.fail("分类名称与分类编码不能为空");
        }
        categoryMapper.insert(category);
        cacheService.evict(Constants.KEY_CATEGORY_LIST);
        return Result.ok("分类新增成功");
    }

    @Override
    @Transactional
    public Result update(Category category) {
        if (category.getId() == null || isBlank(category.getCategoryName())) {
            return Result.fail("参数错误");
        }
        categoryMapper.update(category);
        cacheService.evict(Constants.KEY_CATEGORY_LIST);
        return Result.ok("分类修改成功");
    }

    @Override
    @Transactional
    public Result delete(Long id) {
        if (categoryMapper.countBooks(id) > 0) {
            return Result.fail("该分类下存在图书，无法删除");
        }
        categoryMapper.deleteById(id);
        cacheService.evict(Constants.KEY_CATEGORY_LIST);
        return Result.ok("分类删除成功");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
