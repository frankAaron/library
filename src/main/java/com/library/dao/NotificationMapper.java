package com.library.dao;

import com.library.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {
    int insert(Notification notification);
    List<Notification> selectByUserId(@Param("userId") Long userId);
    long countUnread(@Param("userId") Long userId);
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
    int markAllRead(@Param("userId") Long userId);
    List<Notification> selectAll();
}
