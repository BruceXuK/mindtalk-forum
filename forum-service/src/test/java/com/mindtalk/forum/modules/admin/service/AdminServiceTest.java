package com.mindtalk.forum.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.dto.*;
import com.mindtalk.forum.modules.admin.entity.Permission;
import com.mindtalk.forum.modules.admin.entity.Report;
import com.mindtalk.forum.modules.admin.entity.RolePermission;
import com.mindtalk.forum.modules.admin.mapper.PermissionMapper;
import com.mindtalk.forum.modules.admin.mapper.ReportMapper;
import com.mindtalk.forum.modules.admin.mapper.RolePermissionMapper;
import com.mindtalk.forum.modules.admin.service.impl.AdminServiceImpl;
import com.mindtalk.forum.modules.admin.vo.*;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.user.entity.Role;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserRole;
import com.mindtalk.forum.modules.user.mapper.RoleMapper;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PostMapper postMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TagMapper tagMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private ReportMapper reportMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;
    private Role testRole;
    private Post testPost;
    private Comment testComment;
    private Report testReport;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").email("test@test.com")
                .nickname("Test").status(1).build();
        testRole = Role.builder().id(1L).roleName("超级管理员").roleCode("ADMIN")
                .sortOrder(1).status(1).build();
        testPost = Post.builder().id(1L).title("Test Post").authorId(2L)
                .categoryId(1L).isPinned(false).isFeatured(false)
                .status(1).viewCount(0).likeCount(0).commentCount(0).build();
        testComment = Comment.builder().id(1L).postId(1L).userId(2L)
                .content("Test comment").status(1).likeCount(0).build();
        testReport = Report.builder().id(1L).reporterId(3L).targetType("POST")
                .targetId(1L).reason("垃圾广告").status(1).build();
    }

    @Nested
    @DisplayName("用户管理")
    class UserManageTests {

        @Test
        @DisplayName("分页查询用户")
        void shouldGetUserPage() {
            Page<User> page = new Page<>(1, 20);
            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<User>(1, 20) {{
                        setRecords(List.of(testUser));
                        setTotal(1);
                    }});
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            PageResult<AdminUserVO> result = adminService.getUserPage(null, null, 1, 20);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void shouldThrowWhenUserNotFound() {
            when(userMapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> adminService.getUserDetail(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("不能修改自己的状态")
        void shouldRejectSelfStatusChange() {
            when(userMapper.selectById(1L)).thenReturn(testUser);

            UserManageDTO dto = new UserManageDTO();
            dto.setStatus(0);

            assertThatThrownBy(() -> adminService.updateUserStatus(1L, 1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能修改自己的状态");
        }

        @Test
        @DisplayName("封禁用户")
        void shouldBanUser() {
            when(userMapper.selectById(2L)).thenReturn(
                    User.builder().id(2L).username("target").status(1).build());
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            UserManageDTO dto = new UserManageDTO();
            dto.setStatus(0);

            adminService.updateUserStatus(1L, 2L, dto);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("分配角色")
        void shouldAssignRoles() {
            when(userMapper.selectById(2L)).thenReturn(
                    User.builder().id(2L).username("target").build());
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

            RoleAssignDTO dto = new RoleAssignDTO();
            dto.setRoleIds(List.of(1L));

            adminService.assignUserRoles(1L, 2L, dto);

            verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
            verify(userRoleMapper).insert(any(UserRole.class));
        }
    }

    @Nested
    @DisplayName("帖子审核")
    class PostAuditTests {

        @Test
        @DisplayName("审核帖子")
        void shouldAuditPost() {
            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            PostAuditDTO dto = new PostAuditDTO();
            dto.setStatus(1);

            adminService.auditPost(1L, 1L, dto);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("强制删除帖子")
        void shouldForceDeletePost() {
            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(postMapper.deleteById(1L)).thenReturn(1);

            adminService.forceDeletePost(1L, 1L);

            verify(postMapper).deleteById(1L);
        }

        @Test
        @DisplayName("置顶帖子")
        void shouldPinPost() {
            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            adminService.pinPost(1L, 1L, true, null);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postMapper).updateById(captor.capture());
            assertThat(captor.getValue().getIsPinned()).isTrue();
        }
    }

    @Nested
    @DisplayName("评论审核")
    class CommentAuditTests {

        @Test
        @DisplayName("删除评论")
        void shouldDeleteComment() {
            when(commentMapper.selectById(1L)).thenReturn(testComment);
            when(commentMapper.deleteById(1L)).thenReturn(1);

            adminService.deleteComment(1L, 1L);

            verify(commentMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("举报处理")
    class ReportTests {

        @Test
        @DisplayName("处理举报")
        void shouldHandleReport() {
            when(reportMapper.selectById(1L)).thenReturn(testReport);
            when(reportMapper.updateById(any(Report.class))).thenReturn(1);

            ReportHandleDTO dto = new ReportHandleDTO();
            dto.setStatus(2);
            dto.setHandleResult("已删除帖子");

            adminService.handleReport(1L, 1L, dto);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(2);
            assertThat(captor.getValue().getHandleResult()).isEqualTo("已删除帖子");
        }

        @Test
        @DisplayName("已处理的举报不能重复处理")
        void shouldRejectHandledReport() {
            Report handled = Report.builder().id(1L).reporterId(3L)
                    .targetType("POST").targetId(1L).reason("垃圾广告").status(2).build();
            when(reportMapper.selectById(1L)).thenReturn(handled);

            ReportHandleDTO dto = new ReportHandleDTO();
            dto.setStatus(2);
            dto.setHandleResult("已处理");

            assertThatThrownBy(() -> adminService.handleReport(1L, 1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已处理");
        }
    }

    @Nested
    @DisplayName("权限管理")
    class PermissionTests {

        @Test
        @DisplayName("获取角色列表")
        void shouldGetAllRoles() {
            when(roleMapper.selectList(null)).thenReturn(List.of(testRole));

            List<RoleVO> roles = adminService.getAllRoles();

            assertThat(roles).hasSize(1);
            assertThat(roles.get(0).getRoleCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("获取权限树")
        void shouldGetPermissionTree() {
            Permission perm1 = Permission.builder().id(1L).parentId(0L)
                    .permName("仪表盘").permCode("dashboard").permType(1)
                    .sortOrder(1).status(1).build();
            Permission perm2 = Permission.builder().id(21L).parentId(1L)
                    .permName("查看仪表盘").permCode("dashboard:view").permType(2)
                    .sortOrder(1).status(1).build();
            when(permissionMapper.selectList(null)).thenReturn(List.of(perm1, perm2));

            List<PermissionTreeVO> tree = adminService.getPermissionTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("更新角色权限")
        void shouldUpdateRolePermissions() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(rolePermissionMapper.deleteByRoleId(1L)).thenReturn(1);
            when(rolePermissionMapper.batchInsert(anyList())).thenReturn(1);

            PermissionUpdateDTO dto = new PermissionUpdateDTO();
            dto.setPermissionIds(List.of(1L, 2L));

            adminService.updateRolePermissions(1L, 1L, dto);

            verify(rolePermissionMapper).deleteByRoleId(1L);
            verify(rolePermissionMapper).batchInsert(anyList());
        }
    }

    @Nested
    @DisplayName("统计分析")
    class StatsTests {

        @Test
        @DisplayName("获取统计概览")
        void shouldGetStatsOverview() {
            when(userMapper.selectCount(isNull())).thenReturn(100L);
            when(postMapper.selectCount(isNull())).thenReturn(50L);
            when(commentMapper.selectCount(isNull())).thenReturn(200L);
            when(reportMapper.selectCount(isNull())).thenReturn(20L);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
            when(postMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
            when(commentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
            when(reportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

            StatsOverviewVO overview = adminService.getStatsOverview();

            assertThat(overview.getTotalUsers()).isEqualTo(100L);
            assertThat(overview.getTotalPosts()).isEqualTo(50L);
            assertThat(overview.getTotalComments()).isEqualTo(200L);
            assertThat(overview.getTotalReports()).isEqualTo(20L);
            assertThat(overview.getPendingReports()).isEqualTo(2L);
        }

        @Test
        @DisplayName("获取趋势统计")
        void shouldGetStatsTrends() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(postMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            when(commentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

            List<StatsTrendVO> trends = adminService.getStatsTrends(7);

            assertThat(trends).hasSize(7);
            assertThat(trends.get(0).getDate()).isNotNull();
        }
    }
}
