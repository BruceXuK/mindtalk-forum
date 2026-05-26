package com.mindtalk.forum.modules.post.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.modules.post.document.PostDocument;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.post.repository.PostSearchRepository;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索同步消费者 — 将帖子变更同步到 Elasticsearch
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = Constants.TOPIC_SEARCH_SYNC,
        consumerGroup = "forum-search-sync-consumer-group"
)
public class SearchSyncConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostSearchRepository postSearchRepository;

    @Override
    public void onMessage(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String action = (String) event.get("action");
            Long postId = toLong(event.get("postId"));

            if (postId == null) {
                log.warn("[搜索同步] postId 为空: {}", message);
                return;
            }

            switch (action) {
                case "index" -> indexPost(postId);
                case "delete" -> deletePost(postId);
                default -> log.warn("[搜索同步] 未知 action={}", action);
            }
        } catch (Exception e) {
            log.error("[搜索同步] 处理失败 message={}", message, e);
        }
    }

    private void indexPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() != null && post.getDeleted() != 0) {
            postSearchRepository.deleteById(postId);
            log.info("[搜索同步] 帖子已删除，从 ES 移除 postId={}", postId);
            return;
        }

        User author = userMapper.selectById(post.getAuthorId());
        Category category = post.getCategoryId() != null
                ? categoryMapper.selectById(post.getCategoryId()) : null;
        List<Tag> tags = tagMapper.selectByPostId(postId);

        PostDocument doc = PostDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContentText() != null ? post.getContentText() : post.getContent())
                .authorId(post.getAuthorId())
                .authorName(author != null ? author.getNickname() : null)
                .categoryId(post.getCategoryId())
                .categoryName(category != null ? category.getName() : null)
                .tags(tags.stream()
                        .map(t -> PostDocument.TagRef.builder().id(t.getId()).name(t.getName()).build())
                        .collect(Collectors.toList()))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .isFeatured(post.getIsFeatured())
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .build();

        postSearchRepository.save(doc);
        log.info("[搜索同步] ES 索引成功 postId={} title={}", postId, post.getTitle());
    }

    private void deletePost(Long postId) {
        postSearchRepository.deleteById(postId);
        log.info("[搜索同步] ES 删除成功 postId={}", postId);
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
