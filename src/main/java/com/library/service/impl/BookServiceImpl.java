package com.library.service.impl;

import com.library.common.Constants;
import com.library.common.Result;
import com.library.dao.BookMapper;
import com.library.entity.Book;
import com.library.service.BookService;
import com.library.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图书业务实现
 * <p>
 * 创新点落地：
 * 1. 热门图书榜单使用 Redis ZSet 缓存（member=图书ID, score=累计借阅量），
 * 命中时按榜单顺序返回，未命中回源数据库并重建榜单；
 * 2. 多条件模糊查询：关键词模糊匹配书名/作者/ISBN + 分类/出版社/作者精确筛选；
 * 3. 封面上传校验后缀与大小，UUID 重命名防止路径冲突。
 */
@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    /** 允许上传的封面后缀 */
    private static final List<String> ALLOWED_SUFFIX = Arrays.asList("jpg", "jpeg", "png", "gif");
    /** 封面大小上限 2MB */
    private static final long MAX_COVER_SIZE = 2L * 1024 * 1024;
    /** 热门榜单缓存时长（秒） */
    private static final int HOT_TTL_SECONDS = 1800;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private CacheService cacheService;

    @Override
    public Map<String, Object> page(String keyword, Long categoryId, String publisher, String author,
                                    Integer pageNum, Integer pageSize) {
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int offset = (num - 1) * size;
        List<Book> list = bookMapper.selectPage(trimToNull(keyword), categoryId,
                trimToNull(publisher), trimToNull(author), offset, size);
        long total = bookMapper.countPage(trimToNull(keyword), categoryId,
                trimToNull(publisher), trimToNull(author));
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", (total + size - 1) / size);
        result.put("pageNum", num);
        result.put("pageSize", size);
        return result;
    }

    @Override
    public Book detail(Long id) {
        return bookMapper.selectById(id);
    }

    @Override
    @Transactional
    public Result save(Book book) {
        Result check = validate(book);
        if (!check.isSuccess()) {
            return check;
        }
        if (book.getStock() == null) {
            book.setStock(0);
        }
        if (book.getTotalCount() == null) {
            book.setTotalCount(book.getStock());
        }
        try {
            bookMapper.insert(book);
        } catch (DuplicateKeyException e) {
            return Result.fail("ISBN已存在，请检查后重新录入");
        }
        // 图书推荐影响首页数据，主动失效缓存
        evictHome();
        return Result.ok("图书录入成功");
    }

    @Override
    @Transactional
    public Result update(Book book) {
        if (book.getId() == null) {
            return Result.fail("参数错误");
        }
        Result check = validate(book);
        if (!check.isSuccess()) {
            return check;
        }
        try {
            bookMapper.update(book);
        } catch (DuplicateKeyException e) {
            return Result.fail("ISBN已存在，请检查后重新录入");
        }
        evictHot();
        evictHome();
        return Result.ok("图书信息已更新");
    }

    /** 新增/修改通用字段校验 */
    private Result validate(Book book) {
        if (book.getBookName() == null || book.getBookName().trim().isEmpty()) {
            return Result.fail("书名不能为空");
        }
        if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            return Result.fail("ISBN不能为空");
        }
        if (book.getCategoryId() == null) {
            return Result.fail("请选择图书分类");
        }
        return Result.ok();
    }

    @Override
    @Transactional
    public Result delete(Long id) {
        if (bookMapper.countBorrowingByBook(id) > 0) {
            return Result.fail("该书存在未归还的借阅记录，无法删除");
        }
        bookMapper.deleteById(id);
        evictHot();
        evictHome();
        return Result.ok("图书删除成功");
    }

    @Override
    public String uploadCover(MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择封面图片文件");
        }
        if (file.getSize() > MAX_COVER_SIZE) {
            throw new IllegalArgumentException("封面图片大小不能超过2MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            throw new IllegalArgumentException("封面图片格式不正确");
        }
        String suffix = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SUFFIX.contains(suffix)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/gif 格式图片");
        }
        // 存储到应用发布目录 /uploads 下，UUID 重命名防止重名覆盖
        String dirPath = request.getServletContext().getRealPath("/uploads");
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("上传目录创建失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (Exception e) {
            log.error("封面上传失败", e);
            throw new IllegalStateException("封面上传失败，请稍后重试");
        }
        return "/uploads/" + fileName;
    }

    @Override
    public List<Book> hotTop10() {
        // 1. 命中 ZSet 榜单缓存：按榜单顺序组装图书列表
        Set<String> idSet = cacheService.zRevRange(Constants.KEY_HOT_BOOKS, 0, 9);
        if (idSet != null && !idSet.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (String s : idSet) {
                try {
                    ids.add(Long.parseLong(s));
                } catch (NumberFormatException ignore) {
                }
            }
            if (!ids.isEmpty()) {
                List<Book> books = bookMapper.selectByIds(ids);
                Map<Long, Book> bookMap = new HashMap<>();
                for (Book b : books) {
                    bookMap.put(b.getId(), b);
                }
                List<Book> result = new ArrayList<>(ids.size());
                for (Long id : ids) {
                    Book b = bookMap.get(id);
                    if (b != null) {
                        result.add(b);
                    }
                }
                return result;
            }
        }
        // 2. 缓存未命中：回源数据库查询并重建榜单缓存
        List<Book> hotList = bookMapper.selectHot(10);
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        for (Book b : hotList) {
            scoreMap.put(String.valueOf(b.getId()),
                    b.getBorrowCount() == null ? 0.0 : b.getBorrowCount().doubleValue());
        }
        cacheService.zRebuild(Constants.KEY_HOT_BOOKS, scoreMap, HOT_TTL_SECONDS);
        return hotList;
    }

    @Override
    public List<Book> selectNew(int limit) {
        return bookMapper.selectNew(limit);
    }

    @Override
    public void evictHot() {
        cacheService.evict(Constants.KEY_HOT_BOOKS);
    }

    @Override
    public void evictHome() {
        cacheService.evict(Constants.KEY_HOME_PUBLIC);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}