package com.library.controller;

import com.library.common.Result;
import com.library.entity.User;
import com.library.controller.UserController;
import com.library.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * 借阅业务接口（借阅/归还/续借/预订 全部操作者身份取自 Session）
 */
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    /** 线上借阅 */
    @PostMapping("/apply")
    public Result apply(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return borrowService.borrow(user, param.get("bookId"));
    }

    /** 归还（超期自动计费） */
    @PostMapping("/return")
    public Result returnBook(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return borrowService.returnBook(user, param.get("recordId"));
    }

    /** 一键续借 */
    @PostMapping("/renew")
    public Result renew(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return borrowService.renew(user, param.get("recordId"));
    }

    /** 预订 */
    @PostMapping("/reserve/apply")
    public Result reserve(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return borrowService.reserve(user, param.get("bookId"));
    }

    /** 取消预订 */
    @PostMapping("/reserve/cancel")
    public Result cancelReserve(@RequestBody Map<String, Long> param, HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        return borrowService.cancelReserve(user, param.get("reserveId"));
    }
}