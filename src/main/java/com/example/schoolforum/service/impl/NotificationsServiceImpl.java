package com.example.schoolforum.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.enums.NotificationType;
import com.example.schoolforum.enums.ReadStatus;
import com.example.schoolforum.enums.RelatedType;
import com.example.schoolforum.mapper.NotificationsMapper;
import com.example.schoolforum.pojo.Notifications;
import com.example.schoolforum.service.NotificationsService;
import com.example.schoolforum.util.PermissionUtil;
import com.example.schoolforum.util.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.schoolforum.pojo.table.NotificationsTableDef.NOTIFICATIONS;

/**
 * 系统通知表 服务层实现。
 *
 * @author sugu
 * @since 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsServiceImpl extends ServiceImpl<NotificationsMapper, Notifications> implements NotificationsService {

    private final SseEmitterManager sseEmitterManager;

    @Override
    public List<Notifications> listByUserId(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(NOTIFICATIONS.USER_ID.eq(userId))
                .orderBy(NOTIFICATIONS.CREATED_AT.desc());
        return getMapper().selectListWithRelationsByQuery(wrapper);
    }

    @Override
    public Page<Notifications> list(Long userId, int pageNumber, int pageSize) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(NOTIFICATIONS.USER_ID.eq(userId))
                .orderBy(NOTIFICATIONS.CREATED_AT.desc());
        return getMapper().paginate(pageNumber, pageSize, wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return getMapper().selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATIONS.USER_ID.eq(userId))
                .and(NOTIFICATIONS.IS_READ.eq(ReadStatus.UNREAD)));
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notifications notification = this.getById(notificationId);
        PermissionUtil.checkOwnership(notification != null ? notification.getUserId() : null, userId, "通知");
        
        Notifications update = UpdateEntity.of(Notifications.class, notificationId);
        update.setIsRead(ReadStatus.READ);
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
    }

    @Override
    public void markAllAsRead(Long userId) {
        Notifications notification = UpdateEntity.of(Notifications.class);
        notification.setIsRead(ReadStatus.READ);
        notification.setUpdatedAt(LocalDateTime.now());
        getMapper().updateByQuery(notification, QueryWrapper.create()
                .where(NOTIFICATIONS.USER_ID.eq(userId))
                .and(NOTIFICATIONS.IS_READ.eq(ReadStatus.UNREAD)));
    }

    @Override
    public void createNotification(Long userId, String type, String title, String content, Long relatedId, String relatedType, Long senderId) {
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("无效的通知类型: {}", type);
            return;
        }

        RelatedType notificationRelatedType = null;
        if (relatedType != null) {
            try {
                notificationRelatedType = RelatedType.valueOf(relatedType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的关联类型: {}", relatedType);
                return;
            }
        }

        Notifications notification = Notifications.builder()
                .userId(userId)
                .type(notificationType)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .relatedType(notificationRelatedType)
                .senderId(senderId)
                .isRead(ReadStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        this.save(notification);
        
        sseEmitterManager.sendToUser(userId, notification);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notifications notification = this.getById(notificationId);
        PermissionUtil.checkDeletePermission(notification != null ? notification.getUserId() : null, userId, "通知");
        
        this.removeById(notificationId);
    }
}
