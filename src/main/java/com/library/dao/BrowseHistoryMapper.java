package com.library.dao;

import com.library.entity.BrowseHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 浏览历史 DAO（个性化推荐数据源）
 */
public interface BrowseHistoryMapper {

    /** 新增浏览记录 */
    int insert(BrowseHistory history);

    /** 查询用户近 N 天浏览记录（联查图书分类） */
    List<BrowseHistory> selectRecent(@Param("userId") Long userId,
                                     @Param("days") int days,
                                     @Param("limit") int limit);
}
