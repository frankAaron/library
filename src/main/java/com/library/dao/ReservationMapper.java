package com.library.dao;

import com.library.entity.Reservation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReservationMapper {
    int insert(Reservation reservation);
    List<Reservation> selectMy(@Param("userId") Long userId);
    int cancel(@Param("id") Long id, @Param("userId") Long userId);
    int notifyFirst(@Param("bookId") Long bookId);
    Reservation selectFirstWaitingWithBook(@Param("bookId") Long bookId);
    long existsActive(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<Long> selectActiveBookIds(@Param("userId") Long userId);
    Reservation selectById(@Param("id") Long id);

    /** 自动分配完成，状态置为已完成(status=2) */
    int finish(@Param("id") Long id);
}