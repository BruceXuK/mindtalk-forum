package com.mindtalk.forum.modules.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.common.component.RocketMQProducer;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.common.utils.UserConverter;
import com.mindtalk.forum.modules.comment.dto.CommentQueryDTO;
import com.mindtalk.forum.modules.comment.dto.CreateCommentDTO;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.comment.entity.Like;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.modules.comment.mapper.LikeMapper;
import com.mindtalk.forum.modules.comment.service.CommentService;
import com.mindtalk.forum.modules.comment.vo.CommentVO;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.vo.UserVO;
import com.mindtalk.forum.modules.badge.service.BadgeService;
import com.mindtalk.forum.modules.admin.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final RedisUtils redisUtils;
    private final RocketMQProducer rocketMQProducer;
    private final ObjectMapper objectMapper;
    private final BadgeService badgeService;
    private final SensitiveWordService sensitiveWordService;

    private static final String COMMENT_CACHE_KEY = Constants.REDIS_PREFIX + "comment:post:";
    private static final long COMMENT_CACHE_TTL = 5;
    private static final int MAX_REPLIES_PREVIEW = 3;

    // ════════════════════════ 发表评论 ════════════════════════

    @Override
    @Transactional
    public CommentVO createComment(Long userId, CreateCommentDTO dto) {
        // 校验帖子存在
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        // 如果是二级回复，校验父评论存在
        if (dto.getParentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BusinessException.notFound("父评论不存在");
            }
        }

        Comment comment = Comment.builder()
                .postId(dto.getPostId())
                .userId(userId)
                .content(sensitiveWordService.filter(dto.getContent()))
                .parentId(dto.getParentId())
                .replyToId(dto.getReplyToId())
                .likeCount(0)
                .status(1)
                .build();
        commentMapper.insert(comment);

        // 更新帖子评论数
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("comment_count = comment_count + 1")
                .eq(Post::getId, dto.getPostId());
        postMapper.update(updateWrapper);

        // 清除缓存
        redisUtils.delete(COMMENT_CACHE_KEY + dto.getPostId());

        // 发送通知（异步）
        sendCommentNotification(userId, post, comment);
        // 发送@提及通知
        sendMentionNotification(userId, comment.getId(), dto.getMentionedUserIds(), post.getId(), post.getTitle());

        log.info("[评论] commentId={} postId={} userId={} parentId={}",
                comment.getId(), dto.getPostId(), userId, dto.getParentId());

        badgeService.evaluateAndUnlock(userId);

        // 单条评论：直接查用户构建 VO
        User user = userMapper.selectById(userId);
        User replyToUserEnt = comment.getReplyToId() != null
                ? userMapper.selectById(comment.getReplyToId()) : null;
        return toCommentVO(comment, UserConverter.toUserVO(user), UserConverter.toUserVO(replyToUserEnt), false);
    }

    // ════════════════════════ 分页查询 ════════════════════════

    @Override
    public PageResult<CommentVO> getCommentPage(CommentQueryDTO query, Long currentUserId) {
        // 查缓存（仅缓存第一页）
        if (query.getPage() == 1) {
            PageResult<CommentVO> cached = getCachedComments(query.getPostId());
            if (cached != null) {
                return cached;
            }
        }

        Page<Comment> page = new Page<>(query.getPage(), query.getSize());
        IPage<Comment> result = commentMapper.selectFirstLevelPage(page, query.getPostId(), query.getOrderBy());
        List<Comment> firstLevelComments = result.getRecords();

        if (firstLevelComments.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), query.getPage(), query.getSize());
        }

        // ── 批量加载关联数据 ──

        List<Long> parentIds = firstLevelComments.stream()
                .map(Comment::getId).collect(Collectors.toList());

        // 1. 批量查询所有子回复
        List<Comment> allReplies = commentMapper.selectRepliesByParentIds(parentIds, MAX_REPLIES_PREVIEW);
        Map<Long, List<Comment>> repliesMap = allReplies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 2. 批量统计子回复数
        List<Map<String, Object>> countRows = commentMapper.countRepliesByParentIds(parentIds);
        Map<Long, Integer> replyCountMap = new HashMap<>();
        for (Map<String, Object> row : countRows) {
            Long parentId = ((Number) row.get("parentId")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            replyCountMap.put(parentId, cnt);
        }

        // 3. 收集所有需要查询的用户 ID（一级评论 + 子回复的 userId 和 replyToId）
        Set<Long> allUserIds = new HashSet<>();
        for (Comment c : firstLevelComments) {
            allUserIds.add(c.getUserId());
            if (c.getReplyToId() != null) allUserIds.add(c.getReplyToId());
        }
        for (Comment r : allReplies) {
            allUserIds.add(r.getUserId());
            if (r.getReplyToId() != null) allUserIds.add(r.getReplyToId());
        }

        // 4. 批量加载用户
        Map<Long, User> userMap = Collections.emptyMap();
        if (!allUserIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(new ArrayList<>(allUserIds)).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        }

        // 5. 预查询当前用户的点赞状态
        Set<Long> likedCommentIds = Collections.emptySet();
        if (currentUserId != null) {
            List<Long> allCommentIds = new ArrayList<>(parentIds);
            allReplies.forEach(r -> allCommentIds.add(r.getId()));
            likedCommentIds = new HashSet<>(likeMapper.selectLikedIds(currentUserId, "COMMENT", allCommentIds));
        }

        // ── 构建 VO ──
        final Map<Long, User> fUserMap = userMap;
        final Set<Long> fLikedIds = likedCommentIds;
        List<CommentVO> vos = firstLevelComments.stream()
                .map(c -> {
                    UserVO commentUser = UserConverter.toUserVO(fUserMap.get(c.getUserId()));
                    UserVO replyToVO = c.getReplyToId() != null
                            ? UserConverter.toUserVO(fUserMap.get(c.getReplyToId())) : null;
                    CommentVO vo = toCommentVO(c, commentUser, replyToVO, fLikedIds.contains(c.getId()));
                    // 组装子回复
                    List<Comment> replies = repliesMap.getOrDefault(c.getId(), Collections.emptyList());
                    List<CommentVO> replyVOs = new ArrayList<>();
                    for (Comment r : replies) {
                        UserVO rUser = UserConverter.toUserVO(fUserMap.get(r.getUserId()));
                        UserVO rReplyTo = r.getReplyToId() != null
                                ? UserConverter.toUserVO(fUserMap.get(r.getReplyToId())) : null;
                        replyVOs.add(toCommentVO(r, rUser, rReplyTo, fLikedIds.contains(r.getId())));
                    }
                    vo.setReplies(replyVOs);
                    vo.setReplyCount(replyCountMap.getOrDefault(c.getId(), 0));
                    return vo;
                })
                .collect(Collectors.toList());

        PageResult<CommentVO> pageResult = PageResult.of(vos, result.getTotal(), query.getPage(), query.getSize());

        // 缓存第一页
        if (query.getPage() == 1) {
            cacheComments(query.getPostId(), pageResult);
        }

        return pageResult;
    }

    // ════════════════════════ 查询所有子回复 ════════════════════════

    @Override
    public List<CommentVO> getReplies(Long parentId, Long currentUserId) {
        List<Comment> replies = commentMapper.selectReplies(parentId, 200);
        if (replies.isEmpty()) return Collections.emptyList();

        Set<Long> allUserIds = new HashSet<>();
        for (Comment r : replies) {
            allUserIds.add(r.getUserId());
            if (r.getReplyToId() != null) allUserIds.add(r.getReplyToId());
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(new ArrayList<>(allUserIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Set<Long> likedIds = currentUserId != null
                ? new HashSet<>(likeMapper.selectLikedIds(currentUserId, "COMMENT",
                        replies.stream().map(Comment::getId).collect(Collectors.toList())))
                : Collections.emptySet();

        return replies.stream().map(r -> {
            UserVO ru = userMap.containsKey(r.getUserId()) ? UserConverter.toUserVO(userMap.get(r.getUserId())) : null;
            UserVO rrt = r.getReplyToId() != null && userMap.containsKey(r.getReplyToId())
                    ? UserConverter.toUserVO(userMap.get(r.getReplyToId())) : null;
            return toCommentVO(r, ru, rrt, likedIds.contains(r.getId()));
        }).collect(Collectors.toList());
    }

    // ════════════════════════ 点赞/取消点赞 ════════════════════════

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }

        boolean alreadyLiked = likeMapper.exists(userId, "COMMENT", commentId) > 0;

        if (alreadyLiked) {
            // 取消点赞：软删除
            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, "COMMENT")
                    .eq(Like::getTargetId, commentId);
            Like like = likeMapper.selectOne(wrapper);
            if (like != null) {
                likeMapper.deleteById(like.getId());
            }

            // 更新冗余计数
            LambdaUpdateWrapper<Comment> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Comment::getId, commentId);
            commentMapper.update(updateWrapper);

            log.info("[取消点赞] commentId={} userId={}", commentId, userId);
        } else {
            // 点赞
            Like like = Like.builder()
                    .userId(userId)
                    .targetType("COMMENT")
                    .targetId(commentId)
                    .build();
            likeMapper.insert(like);

            // 更新冗余计数
            LambdaUpdateWrapper<Comment> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("like_count = like_count + 1")
                    .eq(Comment::getId, commentId);
            commentMapper.update(updateWrapper);

            // 发送点赞通知（同步写入）
            if (!comment.getUserId().equals(userId)) {
                User fromUser = userMapper.selectById(userId);
                String fromName = fromUser != null ? fromUser.getNickname() : "用户";
                notificationService.create(Notification.builder()
                        .userId(comment.getUserId()).fromUserId(userId)
                        .notifyType(Constants.NOTIFY_LIKE).title("新的点赞")
                        .content(fromName + " 赞了你的评论").targetType("COMMENT").targetId(comment.getPostId())
                        .isRead(false).build());
            }

            log.info("[点赞] commentId={} userId={}", commentId, userId);
        }

        // 清除帖子评论缓存
        redisUtils.delete(COMMENT_CACHE_KEY + comment.getPostId());
    }

    // ════════════════════════ 删除评论 ════════════════════════

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw BusinessException.forbidden("只能删除自己的评论");
        }

        commentMapper.deleteById(commentId);

        // 更新帖子评论数
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.setSql("comment_count = GREATEST(comment_count - 1, 0)")
                .eq(Post::getId, comment.getPostId());
        postMapper.update(updateWrapper);

        // 清除缓存
        redisUtils.delete(COMMENT_CACHE_KEY + comment.getPostId());

        log.info("[删除评论] commentId={} userId={}", commentId, userId);
    }

    // ════════════════════════ 内部方法 ════════════════════════

    /**
     * 从预加载的数据构建 CommentVO，零额外 SQL
     */
    private CommentVO toCommentVO(Comment c, UserVO userVO,
                                  UserVO replyToVO,
                                  boolean isLiked) {
        return CommentVO.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .user(userVO)
                .parentId(c.getParentId())
                .replyTo(replyToVO)
                .content(c.getContent())
                .likeCount(c.getLikeCount())
                .isLiked(isLiked)
                .createTime(c.getCreateTime())
                .build();
    }

    private void sendCommentNotification(Long userId, Post post, Comment comment) {
        Long toUserId = null;
        boolean isReply = comment.getParentId() != null;
        String title;
        String content;
        User fromUser = userMapper.selectById(userId);
        String fromName = fromUser != null ? fromUser.getNickname() : "用户";

        if (isReply) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null && !parent.getUserId().equals(userId)) {
                toUserId = parent.getUserId();
            }
            title = "评论回复";
            content = fromName + " 回复了你的评论";
        } else {
            if (!post.getAuthorId().equals(userId)) {
                toUserId = post.getAuthorId();
            }
            title = "帖子评论";
            content = fromName + " 评论了你的帖子「" + post.getTitle() + "」";
        }

        if (toUserId != null) {
            notificationService.create(Notification.builder()
                    .userId(toUserId).fromUserId(userId).notifyType(Constants.NOTIFY_COMMENT)
                    .title(title).content(content).targetType("COMMENT").targetId(post.getId())
                    .isRead(false).build());
        }
    }

    private void sendMentionNotification(Long fromUserId, Long commentId, List<Long> mentionedUserIds, Long postId, String postTitle) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) return;
        User fromUser = userMapper.selectById(fromUserId);
        String fromName = fromUser != null ? fromUser.getNickname() : "用户";
        String title = postTitle != null && postTitle.length() > 50
                ? postTitle.substring(0, 50) + "..." : postTitle;
        for (Long mentionedId : mentionedUserIds) {
            if (mentionedId.equals(fromUserId)) continue;
            notificationService.create(Notification.builder()
                    .userId(mentionedId).fromUserId(fromUserId)
                    .notifyType(Constants.NOTIFY_MENTION).title("有人@了你")
                    .content(fromName + " 在评论中提到了你（帖子「" + title + "」）")
                    .targetType("COMMENT").targetId(postId)
                    .isRead(false).build());
        }
    }

    // ──────────────────── 缓存 ────────────────────

    private void cacheComments(Long postId, PageResult<CommentVO> result) {
        try {
            redisUtils.set(COMMENT_CACHE_KEY + postId,
                    objectMapper.writeValueAsString(result),
                    COMMENT_CACHE_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 评论列表序列化失败 postId={}", postId);
        }
    }

    private PageResult<CommentVO> getCachedComments(Long postId) {
        try {
            String json = redisUtils.get(COMMENT_CACHE_KEY + postId);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<PageResult<CommentVO>>() {});
            }
        } catch (Exception e) {
            log.debug("[缓存] 读取评论列表失败 postId={}", postId);
        }
        return null;
    }
}
