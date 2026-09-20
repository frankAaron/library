package com.library.controller;

import com.library.common.Result;
import com.library.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类查询接口（前端下拉筛选使用，列表走 Redis 缓存）
 */
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category/list")
    public Result list() {
        return Result.ok(categoryService.listAll());
    }
}
