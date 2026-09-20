package com.library.dao;

import com.library.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书预订 DAO
 */
public interface ReservationMapper {

    /** 新增预订 */
    int insert(Reservation reservation);

    /** 我的预订列表（联查图书） */
    List<Reservation> selectMy(@Param("userId") Long userId);

    /** 取消预订（仅本人、排队中状态可取消） */
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    /** 图书回库后：按预订时间先后通知最早一条排队中预订，返回受影响行数 */
    int notifyFirst(@Param("bookId") Long bookId);

    /** 校验同一用户对同一图书是否已存在有效预订（排队中/已通知） */
    long existsActive(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
