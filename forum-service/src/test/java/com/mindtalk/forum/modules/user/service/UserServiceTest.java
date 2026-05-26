package com.mindtalk.forum.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.component.RocketMQProducer;
import com.mindtalk.forum.common.utils.JwtUtils;
import com.mindtalk.forum.common.utils.MinioUtils;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.user.dto.*;
import com.mindtalk.forum.modules.user.entity.Role;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserFollow;
import com.mindtalk.forum.modules.user.entity.UserRole;
import com.mindtalk.forum.modules.user.mapper.RoleMapper;
import com.mindtalk.forum.modules.user.mapper.UserFollowMapper;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.mapper.UserRoleMapper;
import com.mindtalk.forum.modules.user.service.impl.UserServiceImpl;
import com.mindtalk.forum.modules.user.vo.LoginResultVO;
import com.mindtalk.forum.modules.user.vo.UserProfileVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserFollowMapper userFollowMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private RedisUtils redisUtils;
    @Mock private MinioUtils minioUtils;
    @Mock private RocketMQProducer rocketMQProducer;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$encoded")
                .nickname("TestUser")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("注册")
    class RegisterTests {

        @Test
        @DisplayName("正常注册成功")
        void shouldRegisterSuccessfully() {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("123456");
            dto.setEmail("new@example.com");
            dto.setNickname("NewUser");

            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(passwordEncoder.encode("123456")).thenReturn("encoded");
            when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return 1;
            });
            Role defaultRole = Role.builder().id(3L).roleCode("USER").roleName("普通用户").status(1).build();
            when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(defaultRole);
            when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);
            when(userFollowMapper.countFollowing(anyLong())).thenReturn(0);
            when(userFollowMapper.countFollower(anyLong())).thenReturn(0);

            UserVO result = userService.register(dto);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newuser");
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("用户名重复应抛冲突异常")
        void shouldThrowConflictWhenUsernameExists() {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("existing");
            dto.setPassword("123456");

            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThatThrownBy(() -> userService.register(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名已被占用");
        }
    }

    @Nested
    @DisplayName("登录")
    class LoginTests {

        @Test
        @DisplayName("正常登录成功，返回令牌")
        void shouldLoginSuccessfully() {
            LoginDTO dto = new LoginDTO();
            dto.setAccount("testuser");
            dto.setPassword("123456");

            doReturn(testUser).when(userMapper).selectOne(any());
            when(passwordEncoder.matches("123456", testUser.getPasswordHash())).thenReturn(true);
            when(jwtUtils.generateAccessToken(1L, "testuser", "USER")).thenReturn("access-token");
            when(jwtUtils.generateRefreshToken(1L)).thenReturn("refresh-token");
            Claims mockClaims = mock(Claims.class);
            when(jwtUtils.parseToken("access-token")).thenReturn(mockClaims);
            when(jwtUtils.parseToken("refresh-token")).thenReturn(mockClaims);
            when(jwtUtils.getAccessTokenTtl()).thenReturn(1800000L);
            when(jwtUtils.getRefreshTokenTtl()).thenReturn(604800000L);
            when(userMapper.update(any(UpdateWrapper.class))).thenReturn(1);
            when(userFollowMapper.countFollowing(anyLong())).thenReturn(5);
            when(userFollowMapper.countFollower(anyLong())).thenReturn(10);

            LoginResultVO result = userService.login(dto);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser()).isNotNull();
            verify(redisUtils).set(eq("mindtalk:auth:access_token:1"), eq("access-token"), anyLong(), any());
        }

        @Test
        @DisplayName("密码错误应抛认证异常")
        void shouldThrowUnauthorizedWhenPasswordWrong() {
            LoginDTO dto = new LoginDTO();
            dto.setAccount("testuser");
            dto.setPassword("wrong");

            when(userMapper.selectOne(any())).thenReturn(testUser);
            when(passwordEncoder.matches("wrong", testUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> userService.login(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("密码错误");
        }
    }

    @Nested
    @DisplayName("关注/取消关注")
    class FollowTests {

        @Test
        @DisplayName("关注成功并发送通知")
        void shouldFollowSuccessfully() {
            when(userMapper.selectById(2L)).thenReturn(
                    User.builder().id(2L).username("other").status(1).build());
            when(userFollowMapper.isFollowing(1L, 2L)).thenReturn(0);
            when(userFollowMapper.insert(any(UserFollow.class))).thenReturn(1);

            assertThatCode(() -> userService.followUser(1L, 2L)).doesNotThrowAnyException();
            verify(rocketMQProducer).sendAsync(eq("follow-event"), anyMap());
        }

        @Test
        @DisplayName("不能关注自己")
        void shouldThrowWhenFollowSelf() {
            assertThatThrownBy(() -> userService.followUser(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能关注自己");
        }

        @Test
        @DisplayName("取消关注成功")
        void shouldUnfollowSuccessfully() {
            UserFollow follow = UserFollow.builder()
                    .id(10L).followerId(1L).followeeId(2L).build();

            when(userFollowMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(follow);
            when(userFollowMapper.deleteById(10L)).thenReturn(1);

            assertThatCode(() -> userService.unfollowUser(1L, 2L)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("用户主页")
    class ProfileTests {

        @Test
        @DisplayName("查看用户主页正常")
        void shouldReturnUserProfile() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userFollowMapper.countFollowing(1L)).thenReturn(10);
            when(userFollowMapper.countFollower(1L)).thenReturn(20);
            when(userFollowMapper.isFollowing(2L, 1L)).thenReturn(1);

            UserProfileVO profile = userService.getUserProfile(1L, 2L);

            assertThat(profile.getUsername()).isEqualTo("testuser");
            assertThat(profile.getFollowingCount()).isEqualTo(10);
            assertThat(profile.getFollowerCount()).isEqualTo(20);
            assertThat(profile.getIsFollowing()).isTrue();
        }
    }
}
