package com.mindtalk.forum.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.component.RocketMQProducer;
import com.mindtalk.forum.common.utils.JwtUtils;
import com.mindtalk.forum.common.utils.MinioUtils;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.common.utils.UserConverter;
import com.mindtalk.forum.modules.user.dto.*;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserFollow;
import com.mindtalk.forum.modules.user.entity.Role;
import com.mindtalk.forum.modules.user.entity.UserRole;
import com.mindtalk.forum.modules.message.entity.Notification;
import com.mindtalk.forum.modules.message.service.NotificationService;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.user.mapper.RoleMapper;
import com.mindtalk.forum.modules.user.mapper.UserFollowMapper;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.mapper.UserRoleMapper;
import com.mindtalk.forum.modules.user.service.UserService;
import com.mindtalk.forum.modules.user.vo.LoginResultVO;
import com.mindtalk.forum.modules.user.vo.UserProfileVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PostMapper postMapper;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final MinioUtils minioUtils;
    private final RocketMQProducer rocketMQProducer;
    private final ObjectMapper objectMapper;

    private static final String USER_CACHE_KEY = Constants.USER_CACHE_KEY;
    private static final long USER_CACHE_TTL_MINUTES = 30;

    // ════════════════════════ 注册 ════════════════════════

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // 检查用户名唯一
        LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(usernameQuery) > 0) {
            throw BusinessException.conflict("用户名已被占用");
        }

        // 检查邮箱唯一
        if (dto.getEmail() != null) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, dto.getEmail());
            if (userMapper.selectCount(emailQuery) > 0) {
                throw BusinessException.conflict("邮箱已被注册");
            }
        }

        User user = User.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .nickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername())
                .status(Constants.USER_STATUS_NORMAL)
                .build();

        userMapper.insert(user);

        // 分配默认角色
        Role defaultRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, Constants.DEFAULT_ROLE));
        if (defaultRole != null) {
            UserRole userRole = UserRole.builder()
                    .userId(user.getId())
                    .roleId(defaultRole.getId())
                    .build();
            userRoleMapper.insert(userRole);
        }

        log.info("[用户注册] userId={} username={}", user.getId(), user.getUsername());
        return toVO(user);
    }

    // ════════════════════════ 登录 ════════════════════════

    @Override
    public LoginResultVO login(LoginDTO dto) {
        // 支持用户名或邮箱登录
        LambdaQueryWrapper<User> loginQuery = new LambdaQueryWrapper<>();
        loginQuery.eq(User::getUsername, dto.getAccount())
                .or()
                .eq(User::getEmail, dto.getAccount());
        User user = userMapper.selectOne(loginQuery);

        if (user == null) {
            throw BusinessException.unauthorized("账号不存在");
        }
        if (user.getStatus() != Constants.USER_STATUS_NORMAL) {
            throw BusinessException.forbidden("账号已被封禁");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("密码错误");
        }

        // 查询用户实际角色（取权重最高的角色）
        String role = resolveUserRole(user.getId());
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), role);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 缓存 token 到 Redis
        redisUtils.set(Constants.AUTH_ACCESS_TOKEN + user.getId(), accessToken,
                jwtUtils.getAccessTokenTtl(), TimeUnit.MILLISECONDS);
        redisUtils.set(Constants.AUTH_REFRESH_TOKEN + user.getId(), refreshToken,
                jwtUtils.getRefreshTokenTtl(), TimeUnit.MILLISECONDS);

        // 更新最后登录（UpdateWrapper 避免 Lambda 序列化）
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("last_login_at", LocalDateTime.now())
                .eq("id", user.getId());
        userMapper.update(updateWrapper);

        log.info("[用户登录] userId={} username={}", user.getId(), user.getUsername());

        return LoginResultVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtils.getAccessTokenTtl() / 1000)
                .user(toVO(user))
                .build();
    }

    // ════════════════════════ 刷新令牌 ════════════════════════

    @Override
    public LoginResultVO refreshToken(RefreshTokenDTO dto) {
        Claims claims = jwtUtils.validateToken(dto.getRefreshToken());
        if (claims == null || !"refresh".equals(claims.get("type", String.class))) {
            throw BusinessException.unauthorized("RefreshToken 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(claims);

        // 校验 Redis 中存储的 refreshToken 是否匹配
        String cachedToken = redisUtils.get(Constants.AUTH_REFRESH_TOKEN + userId);
        if (!dto.getRefreshToken().equals(cachedToken)) {
            throw BusinessException.unauthorized("RefreshToken 已失效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != Constants.USER_STATUS_NORMAL) {
            throw BusinessException.forbidden("账号异常");
        }

        // 令牌轮换：生成新的 access + refresh token
        String role = resolveUserRole(userId);
        String newAccessToken = jwtUtils.generateAccessToken(userId, user.getUsername(), role);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId);

        redisUtils.set(Constants.AUTH_ACCESS_TOKEN + userId, newAccessToken,
                jwtUtils.getAccessTokenTtl(), TimeUnit.MILLISECONDS);
        redisUtils.set(Constants.AUTH_REFRESH_TOKEN + userId, newRefreshToken,
                jwtUtils.getRefreshTokenTtl(), TimeUnit.MILLISECONDS);

        log.debug("[令牌刷新] userId={}", userId);

        return LoginResultVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtils.getAccessTokenTtl() / 1000)
                .user(toVO(user))
                .build();
    }

    // ════════════════════════ 登出 ════════════════════════

    @Override
    public void logout(Long userId, String tokenJti, Date tokenExp) {
        // 删除缓存的 token
        redisUtils.delete(Constants.AUTH_ACCESS_TOKEN + userId);
        redisUtils.delete(Constants.AUTH_REFRESH_TOKEN + userId);

        // 将当前 token 加入黑名单
        if (tokenJti != null) {
            long ttlMs;
            if (tokenExp != null) {
                ttlMs = tokenExp.getTime() - System.currentTimeMillis();
            } else {
                // Gateway 路径（无过期时间），使用 access token 默认 TTL
                ttlMs = jwtUtils.getAccessTokenTtl();
            }
            if (ttlMs > 0) {
                redisUtils.set(Constants.AUTH_BLACKLIST + tokenJti, "1",
                        ttlMs, TimeUnit.MILLISECONDS);
                log.info("[令牌黑名单] jti={} ttl={}ms", tokenJti, ttlMs);
            }
        }

        log.info("[用户登出] userId={}", userId);
    }

    // ════════════════════════ 当前用户 ════════════════════════

    @Override
    public UserVO getCurrentUser(Long userId) {
        UserVO cached = getCachedUser(userId);
        if (cached != null) {
            return cached;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        UserVO vo = toVO(user);
        cacheUser(vo);
        return vo;
    }

    // ════════════════════════ 修改密码 ════════════════════════

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("旧密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 踢出所有登录：删除缓存的 token
        redisUtils.delete(Constants.AUTH_ACCESS_TOKEN + userId);
        redisUtils.delete(Constants.AUTH_REFRESH_TOKEN + userId);

        log.info("[修改密码] userId={} 所有登录已失效", userId);
    }

    // ════════════════════════ 更新资料 ════════════════════════

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId);
        if (dto.getNickname() != null) wrapper.set(User::getNickname, dto.getNickname());
        if (dto.getBio() != null) wrapper.set(User::getBio, dto.getBio());
        if (dto.getGender() != null) wrapper.set(User::getGender, dto.getGender());
        if (dto.getBirthday() != null) wrapper.set(User::getBirthday, dto.getBirthday());
        if (dto.getLocation() != null) wrapper.set(User::getLocation, dto.getLocation());
        userMapper.update(wrapper);

        // 清除缓存
        redisUtils.delete(USER_CACHE_KEY + userId);

        User user = userMapper.selectById(userId);
        log.info("[更新资料] userId={}", userId);
        return toVO(user);
    }

    // ════════════════════════ 头像上传 ════════════════════════

    @Override
    @Transactional
    public UserVO uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.badRequest("只支持上传图片");
        }

        String objectKey = minioUtils.upload(file, "avatar");
        String avatarUrl = minioUtils.getAccessUrl(objectKey);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(User::getAvatarUrl, avatarUrl).eq(User::getId, userId);
        userMapper.update(wrapper);

        redisUtils.delete(USER_CACHE_KEY + userId);

        User user = userMapper.selectById(userId);
        log.info("[头像上传] userId={} key={}", userId, objectKey);
        return toVO(user);
    }

    // ════════════════════════ 关注 ════════════════════════

    @Override
    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw BusinessException.badRequest("不能关注自己");
        }

        // 检查被关注者是否存在
        if (userMapper.selectById(followeeId) == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 检查是否已关注
        if (userFollowMapper.isFollowing(followerId, followeeId) > 0) {
            throw BusinessException.conflict("已关注该用户");
        }

        UserFollow follow = UserFollow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();
        userFollowMapper.insert(follow);

        // 发送关注通知（同步写入）
        User follower = userMapper.selectById(followerId);
        String followerName = follower != null ? follower.getNickname() : "用户";
        notificationService.create(Notification.builder()
                .userId(followeeId).fromUserId(followerId)
                .notifyType(Constants.NOTIFY_FOLLOW).title("新关注")
                .content(followerName + " 关注了你").targetType("USER").targetId(followerId)
                .isRead(false).build());

        log.info("[关注] follower={} followee={}", followerId, followeeId);
    }

    // ════════════════════════ 取消关注 ════════════════════════

    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long followeeId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId);

        UserFollow follow = userFollowMapper.selectOne(wrapper);
        if (follow == null) {
            throw BusinessException.notFound("未关注该用户");
        }

        // 逻辑删除
        userFollowMapper.deleteById(follow.getId());
        log.info("[取消关注] follower={} followee={}", followerId, followeeId);
    }

    // ════════════════════════ 用户主页 ════════════════════════

    @Override
    public UserProfileVO getUserProfile(Long userId, Long currentUserId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        int followingCount = userFollowMapper.countFollowing(userId);
        int followerCount = userFollowMapper.countFollower(userId);
        boolean isFollowing = currentUserId != null
                && userFollowMapper.isFollowing(currentUserId, userId) > 0;

        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .gender(user.getGender())
                .location(user.getLocation())
                .followingCount(followingCount)
                .followerCount(followerCount)
                .postCount(postMapper.selectCount(
                        new LambdaQueryWrapper<Post>()
                                .eq(Post::getAuthorId, userId)
                                .eq(Post::getStatus, Constants.POST_STATUS_NORMAL)).intValue())
                .isFollowing(isFollowing)
                .createTime(user.getCreateTime())
                .build();
    }

    // ════════════════════════ 关注列表 ════════════════════════

    @Override
    public List<UserVO> getFollowingList(Long userId, String keyword, int limit) {
        List<User> users = userFollowMapper.selectFollowingUsers(userId, keyword, limit);
        return users.stream()
                .map(user -> {
                    UserVO vo = UserConverter.toUserVO(user);
                    vo.setFollowingCount(userFollowMapper.countFollowing(user.getId()));
                    vo.setFollowerCount(userFollowMapper.countFollower(user.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ════════════════════════ 用户搜索（@提及） ════════════════════════

    @Override
    public List<UserVO> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword))
                .eq(User::getStatus, Constants.USER_STATUS_NORMAL)
                .last("LIMIT " + Math.min(limit, 20));
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(UserConverter::toUserVO).collect(Collectors.toList());
    }

    // ════════════════════════ 角色检查 ════════════════════════

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        String actualRole = resolveUserRole(userId);
        return roleCode.equals(actualRole);
    }

    // ════════════════════════ 内部方法 ════════════════════════

    private UserVO toVO(User user) {
        UserVO vo = UserConverter.toUserVO(user);
        if (vo == null) return null;
        vo.setFollowingCount(userFollowMapper.countFollowing(user.getId()));
        vo.setFollowerCount(userFollowMapper.countFollower(user.getId()));
        vo.setPostCount(postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getAuthorId, user.getId())
                        .eq(Post::getStatus, Constants.POST_STATUS_NORMAL)).intValue());
        return vo;
    }

    private void cacheUser(UserVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            redisUtils.set(USER_CACHE_KEY + vo.getId(), json,
                    USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 用户序列化失败 userId={}", vo.getId());
        }
    }

    private String resolveUserRole(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (UserRole ur : userRoles) {
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null) {
                if (Constants.ROLE_ADMIN.equals(role.getRoleCode())) {
                    return Constants.ROLE_ADMIN;
                }
                if (Constants.ROLE_MODERATOR.equals(role.getRoleCode())) {
                    return Constants.ROLE_MODERATOR;
                }
            }
        }
        return Constants.DEFAULT_ROLE;
    }

    private UserVO getCachedUser(Long userId) {
        try {
            String json = redisUtils.get(USER_CACHE_KEY + userId);
            if (json != null) {
                return objectMapper.readValue(json, UserVO.class);
            }
        } catch (Exception e) {
            log.debug("[缓存] 读取失败 userId={}", userId);
        }
        return null;
    }
}
