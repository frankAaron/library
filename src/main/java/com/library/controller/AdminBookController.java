package com.library.controller;

import com.library.common.Result;
import com.library.entity.Book;
import com.library.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 管理端-图书管理接口（全部 /admin/** 已由 RoleInterceptor 校验管理员身份）
 */
@RestController
@RequestMapping("/admin/book")
public class AdminBookController {

    @Autowired
    private BookService bookService;

    /** 多条件分页查询 */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       @RequestParam(value = "publisher", required = false) String publisher,
                       @RequestParam(value = "author", required = false) String author,
                       @RequestParam(value = "pageNum", required = false) Integer pageNum,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.ok(bookService.page(keyword, categoryId, publisher, author, pageNum, pageSize));
    }

    /** 新增图书 */
    @PostMapping("/save")
    public Result save(@RequestBody Book book) {
        return bookService.save(book);
    }

    /** 修改图书 */
    @PostMapping("/update")
    public Result update(@RequestBody Book book) {
        return bookService.update(book);
    }

    /** 删除图书 */
    @PostMapping("/delete")
    public Result delete(@RequestBody Map<String, Long> param) {
        return bookService.delete(param.get("id"));
    }

    /** 封面上传，返回图片访问路径 */
    @PostMapping("/uploadCover")
    public Result uploadCover(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return Result.ok("上传成功", bookService.uploadCover(file, request));
    }
}