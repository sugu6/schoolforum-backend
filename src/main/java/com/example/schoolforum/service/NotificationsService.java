package com.example.schoolforum.service;

import com.example.schoolforum.pojo.Notifications;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface NotificationsService extends IService<Notifications> {

    List<Notifications> listByUserId(Long userId);

    Page<Notifications> list(Long userId, int pageNumber, int pageSize);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void createNotification(Long userId, String type, String title, String content, Long relatedId, String relatedType, Long senderId);

    void deleteNotification(Long notificationId, Long userId);
}
