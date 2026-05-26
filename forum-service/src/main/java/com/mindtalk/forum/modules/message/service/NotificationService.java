package com.mindtalk.forum.modules.message.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.vo.NotificationVO;

public interface NotificationService {

    void create(Notification notification);

    PageResult<NotificationVO> getList(Long userId, int page, int size);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    int getUnreadCount(Long userId);
}
