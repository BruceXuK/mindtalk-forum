package com.mindtalk.forum.modules.post.service.impl;

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
import com.mindtalk.forum.modules.post.dto.CreatePostDTO;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.dto.UpdatePostDTO;
import com.mindtalk.forum.modules.comment.entity.Like;
import com.mindtalk.forum.modules.comment.mapper.LikeMapper;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.entity.PostTag;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.mapper.PostTagMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.post.service.PostService;
import com.mindtalk.forum.modules.post.vo.CategoryVO;
import com.mindtalk.forum.modules.post.vo.PostDetailVO;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.post.vo.TagVO;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserFollowMapper;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.service.UserService;
import com.mindtalk.forum.modules.user.vo.UserVO;
import com.mindtalk.forum.modules.badge.service.BadgeService;
import com.mindtalk.forum.modules.reading.mapper.ReadingHistoryMapper;
import com.mindtalk.forum.modules.reading.entity.ReadingHistory;
import com.mindtalk.forum.modules.admin.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostTagMapper postTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final LikeMapper likeMapper;
    private final RedisUtils redisUtils;
    private final RocketMQProducer rocketMQProducer;
    private final ObjectMapper objectMapper;
    private final BadgeService badgeService;
    private final ReadingHistoryMapper readingHistoryMapper;
    private final SensitiveWordService sensitiveWordService;

    private static final String POST_DETAIL_KEY = Constants.REDIS_PREFIX + "post:detail:";
    private static final String POST_HOT_KEY = Constants.REDIS_PREFIX + "post:hot:";
    private static final long POST_DETAIL_TTL = 10;
    private static final long POST_HOT_TTL = 5;

    // ════════════════════════ 发帖 ════════════════════════

    @Override
    @Transactional
    public PostDetailVO createPost(Long authorId, CreatePostDTO dto) {
        boolean isDraft = dto.getStatus() != null && dto.getStatus() == Constants.POST_STATUS_DRAFT;
        int status = isDraft ? Constants.POST_STATUS_DRAFT : Constants.POST_STATUS_NORMAL;

        String filteredTitle = sensitiveWordService.filter(dto.getTitle());
        String filteredContent = sensitiveWordService.filter(dto.getContent());
        String filteredContentText = sensitiveWordService.filter(dto.getContentText());

        Post post = Post.builder()
                .title(filteredTitle)
                .content(filteredContent)
                .contentText(filteredContentText)
                .authorId(authorId)
                .categoryId(dto.getCategoryId())
                .isPinned(false)
                .isFeatured(false)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .collectCount(0)
                .status(status)
                .build();

        postMapper.insert(post);

        // 保存标签关联
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<PostTag> postTags = dto.getTagIds().stream()
                    .map(tagId -> PostTag.builder().postId(post.getId()).tagId(tagId).build())
                    .collect(Collectors.toList());
            postTagMapper.batchInsert(postTags);
        }

        // 草稿不同步 ES，不清理热门缓存
        if (!isDraft) {
            sendSearchSyncEvent("index", post.getId());
            clearHotCache();
            // 发送@提及通知
            sendMentionNotification(authorId, post.getId(), dto.getMentionedUserIds(), post.getTitle());
        }

        log.info("[发帖] postId={} authorId={} title={} status={}", post.getId(), authorId, post.getTitle(), status);
        badgeService.evaluateAndUnlock(authorId);
        return getPostDetail(post.getId(), authorId);
    }

    // ════════════════════════ 编辑帖子 ════════════════════════

    @Override
    @Transactional
    public PostDetailVO updatePost(Long userId, Long postId, UpdatePostDTO dto) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("只能编辑自己的帖子");
        }

        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getContentText() != null) post.setContentText(dto.getContentText());
        if (dto.getCategoryId() != null) post.setCategoryId(dto.getCategoryId());
        postMapper.updateById(post);

        // 更新标签关联
        if (dto.getTagIds() != null) {
            postTagMapper.deleteByPostId(postId);
            if (!dto.getTagIds().isEmpty()) {
                List<PostTag> postTags = dto.getTagIds().stream()
                        .map(tagId -> PostTag.builder().postId(postId).tagId(tagId).build())
                        .collect(Collectors.toList());
                postTagMapper.batchInsert(postTags);
            }
        }

        // 清除缓存
        redisUtils.delete(POST_DETAIL_KEY + postId);
        clearHotCache();

        // 异步同步到 ES
        sendSearchSyncEvent("index", postId);

        log.info("[编辑帖子] postId={} userId={}", postId, userId);
        return getPostDetail(postId, userId);
    }

    // ════════════════════════ 删除帖子 ════════════════════════

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        // 作者或管理员可删除
        if (!post.getAuthorId().equals(userId)) {
            if (!userService.hasRole(userId, Constants.ROLE_ADMIN)) {
                throw BusinessException.forbidden("无权删除此帖子");
            }
        }

        // 软删除帖子
        postMapper.deleteById(postId);
        // 软删除标签关联
        postTagMapper.deleteByPostId(postId);

        // 清除缓存
        redisUtils.delete(POST_DETAIL_KEY + postId);
        clearHotCache();

        // 异步从 ES 移除
        sendSearchSyncEvent("delete", postId);

        log.info("[删除帖子] postId={} userId={}", postId, userId);
    }

    // ════════════════════════ 分页查询 ════════════════════════

    @Override
    public PageResult<PostVO> getPostPage(PostQueryDTO query, Long currentUserId) {
        // 默认排除草稿（status=0），除非调用方明确传入 status
        if (query.getStatus() == null) {
            query.setStatus(Constants.POST_STATUS_NORMAL);
        }
        Page<Post> page = new Page<>(query.getPage(), query.getSize());
        IPage<Post> result = postMapper.selectPageWithAuthor(page,
                query.getCategoryId(), query.getTagId(), query.getKeyword(),
                query.getStatus(), query.getOrderBy(), query.getUserId(),
                query.getFollowingUserId());

        List<Post> posts = result.getRecords();
        if (posts.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), query.getPage(), query.getSize());
        }

        // ── 批量加载关联数据（3 次查询替代 3N 次） ──

        // 1. 批量加载作者
        List<Long> authorIds = posts.stream()
                .map(Post::getAuthorId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 2. 批量加载分类
        List<Long> categoryIds = posts.stream()
                .map(Post::getCategoryId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        // 3. 批量加载标签：先查关联表，再批量查标签
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<PostTag> allPostTags = postTagMapper.selectByPostIds(postIds);
        Map<Long, List<Tag>> tagsMap = Collections.emptyMap();
        if (!allPostTags.isEmpty()) {
            List<Long> tagIds = allPostTags.stream()
                    .map(PostTag::getTagId).distinct().collect(Collectors.toList());
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> a));
            tagsMap = allPostTags.stream()
                    .collect(Collectors.groupingBy(PostTag::getPostId,
                            Collectors.mapping(pt -> tagMap.get(pt.getTagId()),
                                    Collectors.collectingAndThen(Collectors.toList(),
                                            list -> list.stream().filter(Objects::nonNull).collect(Collectors.toList())))));
        }

        // ── 构建 VO ──
        // 4. 批量查询当前用户是否已关注这些作者
        Set<Long> followedAuthorIds = Collections.emptySet();
        if (currentUserId != null && !authorIds.isEmpty()) {
            followedAuthorIds = new HashSet<>(userFollowMapper.selectFolloweeIdsByFollower(currentUserId, authorIds));
        }

        final Map<Long, User> finalUserMap = userMap;
        final Map<Long, Category> finalCategoryMap = categoryMap;
        final Map<Long, List<Tag>> finalTagsMap = tagsMap;
        final Set<Long> finalFollowedAuthorIds = followedAuthorIds;
        List<PostVO> vos = posts.stream()
                .map(post -> toPostVO(post, finalUserMap, finalCategoryMap, finalTagsMap, finalFollowedAuthorIds))
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), query.getPage(), query.getSize());
    }

    // ════════════════════════ 帖子详情 ════════════════════════

    @Override
    public PostDetailVO getPostDetail(Long postId, Long currentUserId) {
        // 查缓存
        PostDetailVO cached = getCachedDetail(postId);
        if (cached != null) {
            return cached;
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        PostDetailVO vo = buildDetailVO(post, currentUserId);

        // 写缓存
        cacheDetail(postId, vo);
        return vo;
    }

    // ════════════════════════ 热门帖子 ════════════════════════

    @Override
    public List<PostVO> getHotPosts(int limit) {
        String cacheKey = POST_HOT_KEY + limit;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<PostVO>>() {});
            } catch (JsonProcessingException e) {
                log.debug("[缓存] 热门帖子反序列化失败");
            }
        }

        List<Post> posts = postMapper.selectHotPosts(limit);
        List<PostVO> vos;
        if (posts.isEmpty()) {
            vos = Collections.emptyList();
        } else {
            // 批量加载关联数据
            List<Long> authorIds = posts.stream().map(Post::getAuthorId).distinct().collect(Collectors.toList());
            Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
            Map<Long, List<Tag>> tagsMap = Collections.emptyMap();
            vos = posts.stream()
                    .map(post -> toPostVO(post, userMap, Collections.emptyMap(), tagsMap, Collections.emptySet()))
                    .collect(Collectors.toList());
        }

        try {
            redisUtils.set(cacheKey, objectMapper.writeValueAsString(vos),
                    POST_HOT_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 热门帖子序列化失败");
        }

        return vos;
    }

    // ════════════════════════ 个性化推荐 ════════════════════════

    @Override
    public List<PostVO> getRecommendedPosts(Long userId, int limit) {
        // 1. 获取用户阅读历史（最近 50 条）
        LambdaQueryWrapper<ReadingHistory> rhWrapper = new LambdaQueryWrapper<>();
        rhWrapper.eq(ReadingHistory::getUserId, userId)
                .orderByDesc(ReadingHistory::getReadAt)
                .last("LIMIT 50");
        List<ReadingHistory> history = readingHistoryMapper.selectList(rhWrapper);

        if (history.isEmpty()) {
            return getHotPosts(limit);
        }

        // 2. 获取已读帖子 ID
        List<Long> readPostIds = history.stream()
                .map(ReadingHistory::getPostId).distinct().collect(Collectors.toList());

        // 3. 加载已读帖子，统计分类频次
        List<Post> readPosts = postMapper.selectBatchIds(readPostIds);
        Map<Long, Long> categoryFreq = readPosts.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(Post::getCategoryId, Collectors.counting()));

        // 4. 取 Top 3 分类
        List<Long> topCategoryIds = categoryFreq.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCategoryIds.isEmpty()) {
            return getHotPosts(limit);
        }

        // 5. 查询这些分类下排除已读的热门帖子
        List<Post> recommendedPosts = new ArrayList<>();
        int perCategory = Math.max(limit / topCategoryIds.size(), 3);
        for (Long catId : topCategoryIds) {
            LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
            postWrapper.eq(Post::getCategoryId, catId)
                    .eq(Post::getStatus, Constants.POST_STATUS_NORMAL)
                    .notIn(Post::getId, readPostIds)
                    .orderByDesc(Post::getViewCount)
                    .last("LIMIT " + perCategory);
            recommendedPosts.addAll(postMapper.selectList(postWrapper));
        }

        if (recommendedPosts.isEmpty()) {
            return getHotPosts(limit);
        }

        // 6. 构建 VO
        List<Post> deduped = recommendedPosts.stream()
                .distinct().limit(limit).collect(Collectors.toList());
        List<Long> authorIds = deduped.stream().map(Post::getAuthorId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return deduped.stream()
                .map(post -> toPostVO(post, userMap, Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    // ════════════════════════ 排行榜 ════════════════════════

    @Override
    public List<PostVO> getRankingPosts(String period, int limit) {
        LocalDateTime startTime;
        LocalDateTime now = LocalDateTime.now();
        if ("weekly".equals(period)) {
            startTime = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
        } else {
            startTime = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        }

        String cacheKey = "post:ranking:" + period + ":" + limit;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<PostVO>>() {});
            } catch (Exception e) {
                log.debug("[缓存] 排行榜反序列化失败");
            }
        }

        List<Post> posts = postMapper.selectRanking(startTime, limit);
        List<PostVO> vos;
        if (posts.isEmpty()) {
            vos = Collections.emptyList();
        } else {
            List<Long> authorIds = posts.stream().map(Post::getAuthorId).distinct().collect(Collectors.toList());
            Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
            vos = posts.stream()
                    .map(post -> toPostVO(post, userMap, Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet()))
                    .collect(Collectors.toList());
        }

        try {
            redisUtils.set(cacheKey, objectMapper.writeValueAsString(vos), 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[缓存] 排行榜序列化失败");
        }
        return vos;
    }

    // ════════════════════════ 相似帖子 ════════════════════════

    @Override
    public List<PostVO> getSimilarPosts(Long postId, int limit) {
        List<PostTag> postTags = postTagMapper.selectByPostId(postId);
        if (postTags.isEmpty()) {
            return getHotPosts(limit);
        }

        List<Long> tagIds = postTags.stream().map(PostTag::getTagId).collect(Collectors.toList());

        LambdaQueryWrapper<PostTag> ptWrapper = new LambdaQueryWrapper<>();
        ptWrapper.in(PostTag::getTagId, tagIds).ne(PostTag::getPostId, postId);
        List<PostTag> related = postTagMapper.selectList(ptWrapper);

        if (related.isEmpty()) {
            return getHotPosts(limit);
        }

        Map<Long, Long> postScore = related.stream()
                .collect(Collectors.groupingBy(PostTag::getPostId, Collectors.counting()));
        List<Long> similarIds = postScore.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Post> posts = postMapper.selectBatchIds(similarIds);
        posts.sort(Comparator.comparingInt(p -> similarIds.indexOf(p.getId())));

        List<Long> authorIds = posts.stream().map(Post::getAuthorId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return posts.stream()
                .map(p -> toPostVO(p, userMap, Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    // ════════════════════════ 点赞 ════════════════════════

    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        boolean alreadyLiked = likeMapper.exists(userId, "POST", postId) > 0;

        if (alreadyLiked) {
            // 取消点赞
            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, "POST")
                    .eq(Like::getTargetId, postId);
            Like like = likeMapper.selectOne(wrapper);
            if (like != null) {
                likeMapper.deleteById(like.getId());
            }

            LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Post::getId, postId);
            postMapper.update(updateWrapper);

            log.info("[取消点赞] postId={} userId={}", postId, userId);
        } else {
            // 点赞
            Like like = Like.builder()
                    .userId(userId)
                    .targetType("POST")
                    .targetId(postId)
                    .build();
            likeMapper.insert(like);

            LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("like_count = like_count + 1")
                    .eq(Post::getId, postId);
            postMapper.update(updateWrapper);

            // 发送点赞通知（同步写入）
            if (!post.getAuthorId().equals(userId)) {
                User author = userMapper.selectById(post.getAuthorId());
                String fromName = userMapper.selectById(userId) != null
                        ? userMapper.selectById(userId).getNickname() : "用户";
                notificationService.create(Notification.builder()
                        .userId(post.getAuthorId()).fromUserId(userId)
                        .notifyType(Constants.NOTIFY_LIKE).title("新的点赞")
                        .content(fromName + " 赞了你的帖子").targetType("POST").targetId(postId)
                        .isRead(false).build());
            }

            log.info("[点赞] postId={} userId={}", postId, userId);
        }

        // 清除缓存
        redisUtils.delete(POST_DETAIL_KEY + postId);
        clearHotCache();
    }

    // ════════════════════════ 收藏 ════════════════════════

    @Override
    @Transactional
    public void collectPost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        boolean alreadyCollected = likeMapper.exists(userId, "POST_COLLECT", postId) > 0;

        if (alreadyCollected) {
            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, "POST_COLLECT")
                    .eq(Like::getTargetId, postId);
            Like like = likeMapper.selectOne(wrapper);
            if (like != null) {
                likeMapper.deleteById(like.getId());
            }

            LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("collect_count = GREATEST(collect_count - 1, 0)")
                    .eq(Post::getId, postId);
            postMapper.update(updateWrapper);

            log.info("[取消收藏] postId={} userId={}", postId, userId);
        } else {
            Like like = Like.builder()
                    .userId(userId)
                    .targetType("POST_COLLECT")
                    .targetId(postId)
                    .build();
            likeMapper.insert(like);

            LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.setSql("collect_count = collect_count + 1")
                    .eq(Post::getId, postId);
            postMapper.update(updateWrapper);

            log.info("[收藏] postId={} userId={}", postId, userId);
        }

        // 清除缓存
        redisUtils.delete(POST_DETAIL_KEY + postId);
    }

    // ════════════════════════ 草稿 ════════════════════════

    @Override
    public PageResult<PostVO> getMyDrafts(Long userId, int page, int size) {
        PostQueryDTO query = new PostQueryDTO();
        query.setPage(page);
        query.setSize(size);
        query.setUserId(userId);
        query.setStatus(Constants.POST_STATUS_DRAFT);
        query.setOrderBy("create_time");
        return getPostPage(query, userId);
    }

    @Override
    @Transactional
    public PostDetailVO publishDraft(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("只能发布自己的草稿");
        }
        if (post.getStatus() != Constants.POST_STATUS_DRAFT) {
            throw BusinessException.badRequest("该帖子不是草稿状态");
        }

        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Post::getStatus, Constants.POST_STATUS_NORMAL)
                .eq(Post::getId, postId);
        postMapper.update(wrapper);

        // 同步到 ES，清除热门缓存
        sendSearchSyncEvent("index", postId);
        clearHotCache();

        log.info("[发布草稿] postId={} userId={}", postId, userId);
        return getPostDetail(postId, userId);
    }

    // ════════════════════════ 记录浏览 ════════════════════════

    private static final String VIEW_COUNT_KEY = Constants.REDIS_PREFIX + "post:view:";

    @Override
    public void recordView(Long postId) {
        String key = VIEW_COUNT_KEY + postId;
        redisUtils.increment(key);
        redisUtils.expire(key, 1, TimeUnit.HOURS);
        log.debug("[浏览] postId={}", postId);
    }

    // ════════════════════════ 内部方法 ════════════════════════

    /**
     * 从预加载的 Map 构建 PostVO，零额外 SQL 查询
     */
    private PostVO toPostVO(Post post, Map<Long, User> userMap,
                            Map<Long, Category> categoryMap,
                            Map<Long, List<Tag>> tagsMap,
                            Set<Long> followedAuthorIds) {
        User user = userMap.get(post.getAuthorId());
        Category category = post.getCategoryId() != null
                ? categoryMap.get(post.getCategoryId()) : null;
        List<Tag> tags = tagsMap.getOrDefault(post.getId(), Collections.emptyList());

        return PostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(summarize(post.getContentText() != null
                        ? post.getContentText() : post.getContent(), 200))
                .author(user != null ? UserConverter.toUserVO(user) : null)
                .category(category != null ? toCategoryVO(category) : null)
                .tags(tags.stream().map(this::toTagVO).collect(Collectors.toList()))
                .viewCount(getMergedViewCount(post.getId(), post.getViewCount() != null ? post.getViewCount() : 0))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .collectCount(post.getCollectCount())
                .authorIsFollowing(followedAuthorIds.contains(post.getAuthorId()))
                .isPinned(post.getIsPinned())
                .isFeatured(post.getIsFeatured())
                .pinnedUntil(post.getPinnedUntil())
                .featuredUntil(post.getFeaturedUntil())
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .build();
    }

    private PostDetailVO buildDetailVO(Post post, Long currentUserId) {
        return PostDetailVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .contentText(post.getContentText())
                .author(UserConverter.toUserVO(userMapper.selectById(post.getAuthorId())))
                .category(post.getCategoryId() != null
                        ? toCategoryVO(categoryMapper.selectById(post.getCategoryId())) : null)
                .tags(tagMapper.selectByPostId(post.getId()).stream()
                        .map(this::toTagVO).collect(Collectors.toList()))
                .viewCount(getMergedViewCount(post.getId(), post.getViewCount() != null ? post.getViewCount() : 0))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .collectCount(post.getCollectCount())
                .isPinned(post.getIsPinned())
                .isFeatured(post.getIsFeatured())
                .pinnedUntil(post.getPinnedUntil())
                .featuredUntil(post.getFeaturedUntil())
                .isLiked(currentUserId != null && likeMapper.exists(currentUserId, "POST", post.getId()) > 0)
                .isCollected(currentUserId != null && likeMapper.exists(currentUserId, "POST_COLLECT", post.getId()) > 0)
                .authorIsFollowing(currentUserId != null && userFollowMapper.isFollowing(currentUserId, post.getAuthorId()) > 0)
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .build();
    }

    private CategoryVO toCategoryVO(Category c) {
        return CategoryVO.builder()
                .id(c.getId()).name(c.getName()).description(c.getDescription())
                .icon(c.getIcon()).sortOrder(c.getSortOrder()).postCount(c.getPostCount())
                .build();
    }

    private TagVO toTagVO(Tag t) {
        return TagVO.builder()
                .id(t.getId()).name(t.getName()).postCount(t.getPostCount())
                .build();
    }

    /** 合并 DB 中的 view_count 和 Redis 实时计数 */
    private int getMergedViewCount(Long postId, int dbViewCount) {
        try {
            String redisVal = redisUtils.get(VIEW_COUNT_KEY + postId);
            if (redisVal != null) {
                return dbViewCount + Integer.parseInt(redisVal);
            }
        } catch (Exception e) {
            // ignore
        }
        return dbViewCount;
    }

    private String summarize(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private void sendSearchSyncEvent(String action, Long postId) {
        Map<String, Object> event = new HashMap<>();
        event.put("action", action);
        event.put("postId", postId);
        rocketMQProducer.sendAsync(Constants.TOPIC_SEARCH_SYNC, event);
    }

    private void clearHotCache() {
        // 清除所有热门缓存变体（limit 可能是 5/10/20）
        for (int limit : new int[]{5, 10, 20}) {
            redisUtils.delete(POST_HOT_KEY + limit);
        }
    }

    private void cacheDetail(Long postId, PostDetailVO vo) {
        try {
            redisUtils.set(POST_DETAIL_KEY + postId,
                    objectMapper.writeValueAsString(vo),
                    POST_DETAIL_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 帖子详情序列化失败 postId={}", postId);
        }
    }

    private void sendMentionNotification(Long fromUserId, Long postId, List<Long> mentionedUserIds, String postTitle) {
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
                    .content(fromName + " 在帖子「" + title + "」中提到了你")
                    .targetType("POST").targetId(postId)
                    .isRead(false).build());
        }
    }

    private PostDetailVO getCachedDetail(Long postId) {
        try {
            String json = redisUtils.get(POST_DETAIL_KEY + postId);
            if (json != null) {
                return objectMapper.readValue(json, PostDetailVO.class);
            }
        } catch (Exception e) {
            log.debug("[缓存] 读取详情失败 postId={}", postId);
        }
        return null;
    }

}
