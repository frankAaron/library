package com.library.service;

import com.library.dao.BookMapper;
import com.library.dao.BorrowRecordMapper;
import com.library.dao.CategoryMapper;
import com.library.dao.FineRecordMapper;
import com.library.dao.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务统计服务（管理端数据看板）
 */
@Service
public class StatsService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BookService bookService;

    /**
     * 管理端总览统计：馆藏/读者/在借/超期/未缴罚款/分类分布/热门TOP10
     */
    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("bookKinds", bookMapper.countAll());
        data.put("bookTotal", bookMapper.sumTotalCount() == null ? 0L
                : bookMapper.sumTotalCount().longValue());
        data.put("readerCount", userMapper.countReaders());
        data.put("borrowing", borrowRecordMapper.countAllBorrowing());
        data.put("overdue", borrowRecordMapper.countOverdue());
        data.put("unpaidFine", fineRecordMapper.sumUnpaid());
        data.put("categoryStats", categoryMapper.selectAll());
        data.put("topBooks", bookService.hotTop10());
        return data;
    }
}
