package com.library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Constants;
import com.library.dao.BookMapper;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.BrowseHistoryMapper;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.BrowseHistory;
import com.library.entity.User;
import com.library.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 轻量化个性化图书推荐服务
 * <p>
 * 算法设计（无第三方依赖，纯 Java 实现）：
 * 1. 采集近90天借阅+浏览行为，按分类聚合偏好分（借阅权重2.0、浏览权重1.0，
 *    并按时间衰减 decay = max(0.1, 1 - 距今天数/90)，近期行为权重更高）；
 * 2. 取偏好 TOP3 分类为候选集来源，排除用户已借阅的图书；
 * 3. 候选图书综合评分 = 分类偏好分归一化×0.6 + 热度分归一化×0.4；
 * 4. 评分倒序取 TOP8 输出，结果缓存1小时；
 * 5. 新用户无行为数据时回退热门榜单，实现千人千面。
 */
@Service
public class RecommendService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** 行为采集窗口（天） */
    private static final int RECO_DAYS = 90;
    /** 推荐输出数量 */
    private static final int RECO_SIZE = 8;
    /** 借阅行为权重 */
    private static final double WEIGHT_BORROW = 2.0;
    /** 浏览行为权重 */
    private static final double WEIGHT_BROWSE = 1.0;
    /** 推荐结果缓存时长（秒） */
    private static final int RECO_TTL = 3600;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookService bookService;

    @Autowired
    private CacheService cacheService;

    /**
     * 个性化推荐入口
     *
     * @param user 当前登录用户（可为 null，回退热门榜单）
     */
    public List<Book> recommend(User user) {
        if (user == null || user.getId() == null) {
            return limitSize(bookService.hotTop10());
        }
        // 1. 缓存优先
        String cacheKey = Constants.KEY_RECO + user.getId();
        String json = cacheService.get(cacheKey);
        if (json != null) {
            try {
                return OBJECT_MAPPER.readValue(json, new TypeReference<List<Book>>() {
                });
            } catch (Exception e) {
                cacheService.evict(cacheKey);
            }
        }
        // 2. 聚合用户近期行为，构建分类偏好分
        Date now = new Date();
        Map<Long, Double> categoryPref = new HashMap<>();
        Set<Long> borrowedBookIds = new HashSet<>();

        List<BorrowRecord> borrowHistories =
                borrowRecordMapper.selectRecentWithBook(user.getId(), RECO_DAYS, 200);
        for (BorrowRecord r : borrowHistories) {
            if (r.getCategoryId() == null) {
                continue;
            }
            borrowedBookIds.add(r.getBookId());
            double decay = decay(DateUtil.diffDays(now, r.getBorrowDate()));
            categoryPref.merge(r.getCategoryId(), WEIGHT_BORROW * decay, Double::sum);
        }
        List<BrowseHistory> browseHistories =
                browseHistoryMapper.selectRecent(user.getId(), RECO_DAYS, 300);
        for (BrowseHistory h : browseHistories) {
            if (h.getCategoryId() == null) {
                continue;
            }
            double decay = decay(DateUtil.diffDays(now, h.getBrowseTime()));
            categoryPref.merge(h.getCategoryId(), WEIGHT_BROWSE * decay, Double::sum);
        }
        // 3. 无行为数据（新用户）→ 热门榜单兜底
        if (categoryPref.isEmpty()) {
            List<Book> fallback = limitSize(bookService.hotTop10());
            cacheService.set(cacheKey, fallback, RECO_TTL);
            return fallback;
        }
        // 4. 偏好 TOP3 分类作为候选集来源（排除已借阅图书）
        List<Long> topCategories = new ArrayList<>();
        categoryPref.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(e -> topCategories.add(e.getKey()));
        List<Book> candidates = bookMapper.selectByCategories(
                topCategories, new ArrayList<>(borrowedBookIds), 60);
        if (candidates.isEmpty()) {
            List<Book> fallback = limitSize(bookService.hotTop10());
            cacheService.set(cacheKey, fallback, RECO_TTL);
            return fallback;
        }
        // 5. 综合评分：偏好分(60%) + 热度分(40%)，倒序取 TOP8
        double maxPref = categoryPref.get(topCategories.get(0));
        int maxBorrowCount = 0;
        for (Book b : candidates) {
            if (b.getBorrowCount() != null && b.getBorrowCount() > maxBorrowCount) {
                maxBorrowCount = b.getBorrowCount();
            }
        }
        double maxHot = Math.max(maxBorrowCount, 1);
        List<Map.Entry<Book, Double>> scored = new ArrayList<>(candidates.size());
        for (Book b : candidates) {
            double prefScore = categoryPref.getOrDefault(b.getCategoryId(), 0.0) / maxPref;
            double hotScore = (b.getBorrowCount() == null ? 0 : b.getBorrowCount()) / maxHot;
            scored.add(new AbstractMap.SimpleEntry<>(b, prefScore * 0.6 + hotScore * 0.4));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Book> result = new ArrayList<>(RECO_SIZE);
        for (Map.Entry<Book, Double> entry : scored) {
            result.add(entry.getKey());
            if (result.size() >= RECO_SIZE) {
                break;
            }
        }
        cacheService.set(cacheKey, result, RECO_TTL);
        return result;
    }

    /** 时间衰减因子：越久远的行为权重越低，最低保留 0.1 */
    private double decay(int days) {
        return Math.max(0.1, 1 - days / (double) RECO_DAYS);
    }

    /** 截断热门榜单为推荐输出数量 */
    private List<Book> limitSize(List<Book> books) {
        if (books.size() <= RECO_SIZE) {
            return books;
        }
        return new ArrayList<>(books.subList(0, RECO_SIZE));
    }
}
