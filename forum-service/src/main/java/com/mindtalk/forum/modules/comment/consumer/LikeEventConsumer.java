package com.mindtalk.forum.modules.comment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.message.service.NotificationSettingService;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 点赞事件消费者 — 生成点赞通知（COMMENT / POST 通用）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.TOPIC_LIKE_EVENT,
        consumerGroup = "forum-like-consumer-group"
)
public class LikeEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final NotificationSettingService settingService;

    @Override
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String type = (String) event.get("type");
            if (!Constants.NOTIFY_LIKE.equals(type)) {
                return;
            }

            Long fromUserId = toLong(event.get("fromUserId"));
            Long toUserId = toLong(event.get("toUserId"));
            String targetType = (String) event.get("targetType");
            Long targetId = toLong(event.get("targetId"));

            if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
                return;
            }

            if (!settingService.isEnabled(toUserId, Constants.NOTIFY_LIKE)) {
                return;
            }

            User fromUser = userMapper.selectById(fromUserId);
            String fromUsername = fromUser != null ? fromUser.getNickname() : "用户";
            String targetLabel = "COMMENT".equals(targetType) ? "评论" : "帖子";

            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .notifyType(Constants.NOTIFY_LIKE)
                    .title("新的点赞")
                    .content(fromUsername + " 赞了你的" + targetLabel)
                    .targetType(targetType)
                    .targetId(targetId)
                    .isRead(false)
                    .build();

            notificationService.create(notification);
            log.info("[点赞通知] fromUserId={} toUserId={} targetType={} targetId={}",
                    fromUserId, toUserId, targetType, targetId);

        } catch (Exception e) {
            log.error("[点赞事件] 处理失败 message={}", message, e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
