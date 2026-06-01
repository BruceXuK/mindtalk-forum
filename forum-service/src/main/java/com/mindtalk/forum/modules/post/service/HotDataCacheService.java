package com.mindtalk.forum.modules.post.service;

import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 热门数据缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotDataCacheService {

    private final RedisUtils redisUtils;
    private final PostMapper postMapper;

    private static final String HOT_POSTS_KEY = Constants.REDIS_PREFIX + "hot:posts:";
    private static final String HOT_CATEGORIES_KEY = Constants.REDIS_PREFIX + "hot:categories:";
    private static final String HOT_TAGS_KEY = Constants.REDIS_PREFIX + "hot:tags:";
    
    private static final int HOT_POSTS_CACHE_TTL = 30;  // 热门帖子缓存 30 分钟
    private static final int HOT_POSTS_LIMIT = 20;     // 热门帖子数量

    /**
     * 获取热门帖子列表（带缓存）
     */
    public List<PostVO> getHotPosts() {
        String cacheKey = HOT_POSTS_KEY + "list";
        String cached = redisUtils.get(cacheKey);
        
        if (cached != null) {
            log.debug("[缓存] 命中热门帖子缓存");
            return parseCachedPosts(cached);
        }
        
        // 缓存未命中，从数据库查询
        List<PostVO> hotPosts = queryHotPostsFromDB();
        
        // 写入缓存
        cachePosts(cacheKey, hotPosts);
        
        return hotPosts;
    }

    /**
     * 刷新热门帖子缓存
     */
    @Scheduled(fixedRate = 1800000)  // 每 30 分钟执行一次
    public void refreshHotPostsCache() {
        try {
            log.info("[缓存] 刷新热门帖子缓存...");
            List<PostVO> hotPosts = queryHotPostsFromDB();
            String cacheKey = HOT_POSTS_KEY + "list";
            cachePosts(cacheKey, hotPosts);
            log.info("[缓存] 热门帖子缓存刷新完成，共 {} 条", hotPosts.size());
        } catch (Exception e) {
            log.error("[缓存] 刷新热门帖子缓存失败", e);
        }
    }

    /**
     * 帖子数据变更时清除缓存
     */
    public void invalidateHotPostsCache() {
        String cacheKey = HOT_POSTS_KEY + "list";
        redisUtils.delete(cacheKey);
        log.debug("[缓存] 清除热门帖子缓存");
    }

    /**
     * 从数据库查询热门帖子
     */
    private List<PostVO> queryHotPostsFromDB() {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, Constants.POST_STATUS_NORMAL)
                .eq(Post::getDeleted, Constants.NOT_DELETED)
                .orderByDesc(Post::getViewCount)
                .orderByDesc(Post::getLikeCount)
                .last("LIMIT " + HOT_POSTS_LIMIT);
        
        List<Post> posts = postMapper.selectList(wrapper);
        return posts.stream().map(this::toPostVO).collect(Collectors.toList());
    }

    private List<PostVO> parseCachedPosts(String cached) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cached, 
                mapper.getTypeFactory().constructCollectionType(List.class, PostVO.class));
        } catch (Exception e) {
            log.warn("[缓存] 解析热门帖子缓存失败", e);
            return queryHotPostsFromDB();
        }
    }

    private void cachePosts(String cacheKey, List<PostVO> posts) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(posts);
            redisUtils.set(cacheKey, json, HOT_POSTS_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[缓存] 写入热门帖子缓存失败", e);
        }
    }

    private PostVO toPostVO(Post post) {
        return PostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createTime(post.getCreateTime())
                .build();
    }
}
