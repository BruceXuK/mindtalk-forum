package com.mindtalk.forum.modules.post.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 浏览统计消费者 — Redis 计数 + 定期刷新到 DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.TOPIC_VIEW_COUNT,
        consumerGroup = "forum-view-count-consumer-group"
)
public class ViewCountConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final RedisUtils redisUtils;
    private final PostMapper postMapper;

    private static final String VIEW_COUNT_KEY = Constants.REDIS_PREFIX + "post:view:";

    @Value("${forum.view-count-flush-threshold:100}")
    private long flushThreshold;

    @Value("${forum.view-count-expire-hours:1}")
    private int expireHours;

    @Override
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            Long postId = toLong(event.get("postId"));
            if (postId == null) {
                return;
            }

            String key = VIEW_COUNT_KEY + postId;
            long count = redisUtils.increment(key);
            redisUtils.expire(key, expireHours, TimeUnit.HOURS);

            log.debug("[浏览统计] postId={} count={}", postId, count);

            // 达到阈值时刷新到 DB
            if (count % flushThreshold == 0) {
                log.info("[浏览统计] 达到刷新阈值 postId={} totalViews={}", postId, count);
                try {
                    Post post = postMapper.selectById(postId);
                    if (post != null) {
                        post.setViewCount((int) count);
                        postMapper.updateById(post);
                        log.debug("[浏览统计] 已刷新到DB postId={} viewCount={}", postId, count);
                    }
                } catch (Exception e) {
                    log.error("[浏览统计] 刷新DB失败 postId={}", postId, e);
                }
            }

        } catch (Exception e) {
            log.error("[浏览统计] 处理失败 message={}", message, e);
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
