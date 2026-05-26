package com.mindtalk.forum.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.common.utils.UserConverter;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.mapper.NotificationMapper;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.message.vo.NotificationVO;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public void create(Notification notification) {
        notificationMapper.insert(notification);
        log.debug("[通知] 创建通知 id={} type={} toUserId={}",
                notification.getId(), notification.getNotifyType(), notification.getUserId());
    }

    @Override
    public PageResult<NotificationVO> getList(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> p = new Page<>(page, Math.min(size, 50));
        IPage<Notification> result = notificationMapper.selectPage(p, wrapper);

        Set<Long> fromUserIds = result.getRecords().stream()
                .map(Notification::getFromUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, User> userMap;
        if (!fromUserIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(new ArrayList<>(fromUserIds)).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        } else {
            userMap = Collections.emptyMap();
        }

        List<NotificationVO> vos = result.getRecords().stream().map(n -> NotificationVO.builder()
                .id(n.getId()).userId(n.getUserId())
                .fromUser(n.getFromUserId() != null ? UserConverter.toUserVO(userMap.get(n.getFromUserId())) : null)
                .notifyType(n.getNotifyType()).title(n.getTitle()).content(n.getContent())
                .targetType(n.getTargetType()).targetId(n.getTargetId())
                .isRead(n.getIsRead()).createTime(n.getCreateTime())
                .build()).collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadAt, LocalDateTime.now());
        notificationMapper.update(wrapper);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadAt, LocalDateTime.now());
        notificationMapper.update(wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId).eq(Notification::getIsRead, false);
        return notificationMapper.selectCount(wrapper).intValue();
    }
}
