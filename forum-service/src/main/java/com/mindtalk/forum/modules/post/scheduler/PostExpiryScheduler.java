package com.mindtalk.forum.modules.post.scheduler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务：自动取消过期的置顶和加精
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostExpiryScheduler {

    private final PostMapper postMapper;

    @Scheduled(cron = "${scheduling.post-expiry-cron:0 */30 * * * *}")
    public void expirePinnedPosts() {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Post::getIsPinned, false)
                .isNotNull(Post::getPinnedUntil)
                .lt(Post::getPinnedUntil, now)
                .eq(Post::getIsPinned, true);

        int count = postMapper.update(wrapper);
        if (count > 0) {
            log.info("[定时任务] 取消{}篇过期置顶帖子", count);
        }
    }

    @Scheduled(cron = "${scheduling.post-expiry-cron:0 */30 * * * *}")
    public void expireFeaturedPosts() {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Post::getIsFeatured, false)
                .isNotNull(Post::getFeaturedUntil)
                .lt(Post::getFeaturedUntil, now)
                .eq(Post::getIsFeatured, true);

        int count = postMapper.update(wrapper);
        if (count > 0) {
            log.info("[定时任务] 取消{}篇过期加精帖子", count);
        }
    }
}
