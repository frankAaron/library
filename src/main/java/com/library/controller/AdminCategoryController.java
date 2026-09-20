package com.library.controller;

import com.library.common.Result;
import com.library.entity.Category;
import com.library.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-图书分类管理接口
 */
@RestController
@RequestMapping("/admin/category")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result list() {
        return Result.ok(categoryService.listAll());
    }

    @PostMapping("/save")
    public Result save(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Category category) {
        return categoryService.update(category);
    }

    @PostMapping("/delete")
    public Result delete(@RequestBody Map<String, Long> param) {
        return categoryService.delete(param.get("id"));
    }
}
