package com.library.controller;

import com.library.entity.Book;
import com.library.entity.User;
import com.library.service.AccountService;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.CategoryService;
import com.library.service.PermissionService;
import com.library.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

/**
 * 页面路由控制器（JSP 视图跳转，数据由各业务接口提供）
 */
@Controller
public class PageController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private StatsService statsService;

    /* ==================== 读者端页面 ==================== */

    /** 登录页 */
    @GetMapping("/toLogin")
    public String toLogin() {
        return "login";
    }

    /** 注册页 */
    @GetMapping("/toRegister")
    public String toRegister() {
        return "register";
    }

    /** 图书查询页（多条件筛选 + 分页，服务端渲染） */
    @GetMapping("/book/toBooks")
    public String toBooks(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "categoryId", required = false) Long categoryId,
                          @RequestParam(value = "publisher", required = false) String publisher,
                          @RequestParam(value = "author", required = false) String author,
                          @RequestParam(value = "pageNum", required = false) Integer pageNum,
                          Model model) {
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("pageData", bookService.page(keyword, categoryId, publisher, author, pageNum, 10));
        // 回显筛选条件
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("publisher", publisher);
        model.addAttribute("author", author);
        return "reader/books";
    }

    /** 图书详情页（登录读者自动记录浏览历史，供推荐算法使用） */
    @GetMapping("/book/detail/{id}")
    public String bookDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        Book book = bookService.detail(id);
        if (book == null) {
            return "redirect:/book/toBooks";
        }
        model.addAttribute("book", book);
        model.addAttribute("category", categoryService.selectById(book.getCategoryId()));
        User user = (User) session.getAttribute("loginUser");
        if (user != null && user.getRole() != null && user.getRole() > 0) {
            borrowService.recordBrowse(user.getId(), id);
            model.addAttribute("myBorrow", borrowService.currentBorrowing(user.getId(), id));
        }
        return "reader/bookDetail";
    }

    /** 我的借阅页（借阅记录/预订/罚款/押金） */
    @GetMapping("/borrow/toMyBorrow")
    public String toMyBorrow(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("borrows", borrowService.myBorrows(user.getId()));
        model.addAttribute("reservations", borrowService.myReservations(user.getId()));
        model.addAttribute("fines", accountService.myFines(user.getId()));
        model.addAttribute("depositRecords", accountService.myDepositRecords(user.getId()));
        model.addAttribute("permUser", permissionService.getUserCached(user.getId()));
        return "reader/myBorrow";
    }

    /** 个人中心（资料/密码/押金充值） */
    @GetMapping("/user/toProfile")
    public String toProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("permUser", permissionService.getUserCached(user.getId()));
        model.addAttribute("depositRecords", accountService.myDepositRecords(user.getId()));
        return "reader/profile";
    }

    /* ==================== 管理员端页面 ==================== */

    /** 后台首页（统计看板） */
    @GetMapping("/admin/toMain")
    public String toAdminMain(Model model) {
        model.addAttribute("stats", statsService.overview());
        return "admin/main";
    }

    /** 图书管理页 */
    @GetMapping("/admin/toBooks")
    public String toAdminBooks(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "admin/books";
    }

    /** 图书编辑页（id为空表示新增） */
    @GetMapping("/admin/toBookEdit")
    public String toBookEdit(@RequestParam(value = "id", required = false) Long id, Model model) {
        model.addAttribute("book", id == null ? null : bookService.detail(id));
        model.addAttribute("categories", categoryService.listAll());
        return "admin/bookEdit";
    }

    /** 分类管理页 */
    @GetMapping("/admin/toCategories")
    public String toCategories(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "admin/categories";
    }

    /** 读者管理页 */
    @GetMapping("/admin/toUsers")
    public String toUsers() {
        return "admin/users";
    }

    /** 借阅记录管理页 */
    @GetMapping("/admin/toBorrows")
    public String toBorrows() {
        return "admin/borrows";
    }

    /** 罚款对账页 */
    @GetMapping("/admin/toFines")
    public String toFines() {
        return "admin/fines";
    }
}