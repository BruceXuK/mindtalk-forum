package com.mindtalk.forum.modules.comment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.common.service.EmailService;
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
 * 评论事件消费者 — 生成评论通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.TOPIC_COMMENT_EVENT,
        consumerGroup = "forum-comment-consumer-group"
)
public class CommentEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;
    private final NotificationSettingService settingService;
    private final EmailService emailService;

    @Override
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            Long fromUserId = toLong(event.get("fromUserId"));
            Long toUserId = toLong(event.get("toUserId"));
            Long commentId = toLong(event.get("commentId"));
            String postTitle = (String) event.get("postTitle");

            if (fromUserId == null || toUserId == null || commentId == null) {
                return;
            }

            if (!settingService.isEnabled(toUserId, Constants.NOTIFY_COMMENT)) {
                return;
            }

            User fromUser = userMapper.selectById(fromUserId);
            String fromUsername = fromUser != null ? fromUser.getNickname() : "用户";

            Comment comment = commentMapper.selectById(commentId);
            boolean isReply = comment != null && comment.getParentId() != null;
            String title = isReply ? "评论回复" : "帖子评论";
            String content = isReply
                    ? fromUsername + " 回复了你的评论"
                    : fromUsername + " 评论了你的帖子「" + (postTitle != null ? postTitle : "") + "」";

            Notification notification = Notification.builder()
                    .userId(toUserId)
                    .fromUserId(fromUserId)
                    .notifyType(Constants.NOTIFY_COMMENT)
                    .title(title)
                    .content(content)
                    .targetType("COMMENT")
                    .targetId(commentId)
                    .isRead(false)
                    .build();

            notificationService.create(notification);

            User toUser = userMapper.selectById(toUserId);
            if (toUser != null && toUser.getEmail() != null && !toUser.getEmail().isEmpty()) {
                emailService.sendSimpleEmail(toUser.getEmail(),
                        "【MindTalk】" + title,
                        content + "\n\n查看详情: https://mindtalk.example.com");
            }

            log.info("[评论通知] fromUserId={} toUserId={} commentId={}", fromUserId, toUserId, commentId);

        } catch (Exception e) {
            log.error("[评论事件] 处理失败 message={}", message, e);
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
