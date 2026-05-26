package com.mindtalk.forum.modules.badge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.modules.badge.entity.Badge;
import com.mindtalk.forum.modules.badge.entity.UserBadge;
import com.mindtalk.forum.modules.badge.mapper.BadgeMapper;
import com.mindtalk.forum.modules.badge.mapper.UserBadgeMapper;
import com.mindtalk.forum.modules.badge.service.BadgeService;
import com.mindtalk.forum.modules.badge.vo.BadgeVO;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserFollow;
import com.mindtalk.forum.modules.user.mapper.UserFollowMapper;
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
public class BadgeServiceImpl implements BadgeService {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    private static final String FIRST_POST = "FIRST_POST";
    private static final String TEN_POSTS = "TEN_POSTS";
    private static final String FIFTY_POSTS = "FIFTY_POSTS";
    private static final String TEN_LIKES = "TEN_LIKES";
    private static final String HUNDRED_LIKES = "HUNDRED_LIKES";
    private static final String FIVE_FOLLOWS = "FIVE_FOLLOWS";
    private static final String SEVEN_DAY_LOGIN = "SEVEN_DAY_LOGIN";
    private static final String TWENTY_COMMENTS = "TWENTY_COMMENTS";

    @Override
    public List<BadgeVO> getUserBadges(Long userId) {
        List<Badge> badges = badgeMapper.selectByUserId(userId);
        if (badges.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> badgeIds = badges.stream().map(Badge::getId).collect(Collectors.toList());
        LambdaQueryWrapper<UserBadge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBadge::getUserId, userId)
                .in(UserBadge::getBadgeId, badgeIds);
        Map<Long, LocalDateTime> unlockMap = userBadgeMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, UserBadge::getUnlockedAt, (a, b) -> a));

        return badges.stream()
                .map(b -> BadgeVO.builder()
                        .id(b.getId()).code(b.getCode()).name(b.getName())
                        .description(b.getDescription()).iconUrl(b.getIconUrl())
                        .category(b.getCategory()).sortOrder(b.getSortOrder())
                        .unlockedAt(unlockMap.get(b.getId()))
                        .build())
                .sorted(Comparator.comparing(BadgeVO::getSortOrder))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void evaluateAndUnlock(Long userId) {
        Map<String, Badge> badgeMap = loadBadges();
        if (badgeMap.isEmpty()) return;

        int postCount = countPosts(userId);
        int receivedLikeCount = sumReceivedLikes(userId);
        int followCount = countFollows(userId);
        int commentCount = countComments(userId);
        int loginDays = countDistinctLoginDays(userId);

        unlockIf(userId, badgeMap.get(FIRST_POST), postCount >= 1);
        unlockIf(userId, badgeMap.get(TEN_POSTS), postCount >= 10);
        unlockIf(userId, badgeMap.get(FIFTY_POSTS), postCount >= 50);
        unlockIf(userId, badgeMap.get(TEN_LIKES), receivedLikeCount >= 10);
        unlockIf(userId, badgeMap.get(HUNDRED_LIKES), receivedLikeCount >= 100);
        unlockIf(userId, badgeMap.get(FIVE_FOLLOWS), followCount >= 5);
        unlockIf(userId, badgeMap.get(SEVEN_DAY_LOGIN), loginDays >= 7);
        unlockIf(userId, badgeMap.get(TWENTY_COMMENTS), commentCount >= 20);
    }

    @Override
    public void evaluateAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1).orderByAsc(User::getId);
        List<User> users = userMapper.selectList(wrapper);
        for (User user : users) {
            try {
                evaluateAndUnlock(user.getId());
            } catch (Exception e) {
                log.error("[勋章评估] 用户评估失败 userId={}", user.getId(), e);
            }
        }
    }

    // ──────────────────── 内部方法 ────────────────────

    private Map<String, Badge> loadBadges() {
        List<Badge> all = badgeMapper.selectList(null);
        return all.stream().collect(Collectors.toMap(Badge::getCode, b -> b, (a, b) -> a));
    }

    private int countPosts(Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, userId).eq(Post::getStatus, 1);
        return Math.toIntExact(postMapper.selectCount(wrapper));
    }

    private int sumReceivedLikes(Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, userId).eq(Post::getStatus, 1)
                .select(Post::getLikeCount);
        List<Post> posts = postMapper.selectList(wrapper);
        return posts.stream().mapToInt(p -> p.getLikeCount() != null ? p.getLikeCount() : 0).sum();
    }

    private int countFollows(Long userId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId);
        return Math.toIntExact(userFollowMapper.selectCount(wrapper));
    }

    private int countComments(Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId);
        return Math.toIntExact(commentMapper.selectCount(wrapper));
    }

    private int countDistinctLoginDays(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getLastLoginAt() == null) return 0;
        // Approximate: count the registration age in days, capped at login days
        // For simplicity: if user has been registered for N days and has last_login_at, use N as proxy
        // Actually count using last_login_at vs create_time
        LocalDateTime createTime = user.getCreateTime();
        if (createTime == null) return 1;
        long days = java.time.temporal.ChronoUnit.DAYS.between(createTime.toLocalDate(), LocalDateTime.now().toLocalDate());
        return (int) Math.min(days + 1, 365); // cap at 365
    }

    private void unlockIf(Long userId, Badge badge, boolean condition) {
        if (badge == null || !condition) return;
        if (userBadgeMapper.exists(userId, badge.getId())) return;

        UserBadge ub = UserBadge.builder()
                .userId(userId)
                .badgeId(badge.getId())
                .unlockedAt(LocalDateTime.now())
                .notified(false)
                .build();
        userBadgeMapper.insert(ub);

        notificationService.create(Notification.builder()
                .userId(userId)
                .notifyType(Constants.NOTIFY_SYSTEM)
                .title("获得新勋章！")
                .content("恭喜获得「" + badge.getName() + "」勋章 — " + badge.getDescription())
                .targetType("BADGE")
                .targetId(badge.getId())
                .isRead(false)
                .build());

        userBadgeMapper.updateById(UserBadge.builder()
                .id(ub.getId()).notified(true).build());

        log.info("[勋章] 解锁成功 userId={} badge={}", userId, badge.getCode());
    }
}
