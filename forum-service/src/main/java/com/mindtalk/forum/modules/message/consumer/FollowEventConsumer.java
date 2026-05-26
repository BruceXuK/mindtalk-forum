package com.mindtalk.forum.modules.message.consumer;

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
 * 关注事件消费者 — 关注后生成通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.TOPIC_FOLLOW_EVENT,
        consumerGroup = "forum-follow-consumer-group"
)
public class FollowEventConsumer implements RocketMQListener<String> {

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
            if (!Constants.NOTIFY_FOLLOW.equals(type)) {
                return;
            }

            Long fromUserId = toLong(event.get("fromUserId"));
            Long toUserId = toLong(event.get("toUserId"));

            if (fromUserId == null || toUserId == null) {
                log.warn("[关注事件] 消息参数不完整: {}", message);
                return;
            }

            if (!settingService.isEnabled(toUserId, Constants.NOTIFY_FOLLOW)) {
                return;
            }

            User fromUser = userMapper.selectById(fromUserId);
            String fromUsername = fromUser != null ? fromUser.getUsername() : "未知用户";

            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .notifyType(Constants.NOTIFY_FOLLOW)
                    .title("新关注")
                    .content(fromUsername + " 关注了你")
                    .targetType("USER")
                    .targetId(fromUserId)
                    .isRead(false)
                    .build();

            notificationService.create(notification);
            log.info("[关注事件] 通知已生成 fromUserId={} toUserId={}", fromUserId, toUserId);

        } catch (Exception e) {
            log.error("[关注事件] 处理失败 message={}", message, e);
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
