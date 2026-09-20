package com.library.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.library.common.Constants;
import com.library.dao.BorrowRecordMapper;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.User;
import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.CacheService;
import com.library.service.CategoryService;
import com.library.service.PermissionService;
import com.library.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 首页控制器（动态首页数据渲染）
 * <p>
 * 动态渲染策略（千人千面）：
 * - 公共部分：分类导航、热门借阅榜TOP10（Redis ZSet缓存）、图书推荐（Redis缓存15分钟）；
 * - 登录用户追加：个性化推荐、借阅概况（在借数量/额度/累计借阅）、到期预警横幅；
 * - 活跃读者（累计借阅≥5次）展示专属活跃徽章文案。
 */
@Controller
public class HomeController {

    /** 活跃读者累计借阅次数门槛 */
    private static final long ACTIVE_READER_THRESHOLD = 5;
    /** 首页新书数量 */
    private static final int NEW_BOOK_SIZE = 8;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @GetMapping({"/", "/index"})
    public String index(HttpSession session, Model model) {
        /* ---------- 公共数据（缓存加速） ---------- */
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("hotBooks", bookService.hotTop10());
        model.addAttribute("newBooks", cacheService.getOrLoad(Constants.KEY_HOME_PUBLIC, 900,
                new TypeReference<List<Book>>() {
                }, () -> bookService.selectNew(NEW_BOOK_SIZE)));

        /* ---------- 登录用户动态数据 ---------- */
        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser != null) {
            User permUser = permissionService.getUserCached(sessionUser.getId());
            if (permUser != null) {
                model.addAttribute("permUser", permUser);
                model.addAttribute("borrowingCount", borrowRecordMapper.countBorrowing(permUser.getId()));
                model.addAttribute("totalBorrow", borrowRecordMapper.countTotal(permUser.getId()));
            }
            // 到期预警（含已超期），首页顶部黄色横幅提醒
            List<BorrowRecord> dueWarn = borrowService.dueSoon(sessionUser.getId());
            model.addAttribute("dueWarn", dueWarn);
            // 个性化推荐（无行为数据时内部自动回退热门榜单）
            model.addAttribute("recoList", recommendService.recommend(sessionUser));
            model.addAttribute("activeReader",
                    sessionUser.getRole() != null && sessionUser.getRole() > 0
                            && borrowRecordMapper.countTotal(sessionUser.getId()) >= ACTIVE_READER_THRESHOLD);
        }
        return "index";
    }
}