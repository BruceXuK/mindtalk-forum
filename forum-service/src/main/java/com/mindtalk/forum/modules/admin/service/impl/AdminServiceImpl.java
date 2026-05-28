package com.mindtalk.forum.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.annotation.AdminLog;
import com.mindtalk.forum.modules.admin.dto.*;
import com.mindtalk.forum.modules.admin.entity.Permission;
import com.mindtalk.forum.modules.admin.entity.Report;
import com.mindtalk.forum.modules.admin.entity.RolePermission;
import com.mindtalk.forum.modules.admin.mapper.AdminLogMapper;
import com.mindtalk.forum.modules.admin.mapper.PermissionMapper;
import com.mindtalk.forum.modules.dict.service.DictService;
import com.mindtalk.forum.modules.admin.mapper.ReportMapper;
import com.mindtalk.forum.modules.admin.mapper.RolePermissionMapper;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.*;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.modules.comment.vo.CommentVO;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.user.entity.Role;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserRole;
import com.mindtalk.forum.modules.user.mapper.RoleMapper;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.mapper.UserRoleMapper;
import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final ReportMapper reportMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminLogMapper adminLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final DictService dictService;
    private final com.mindtalk.forum.modules.message.service.NotificationService notificationService;

    // ════════════════════════ 用户管理 ════════════════════════

    @Override
    public PageResult<AdminUserVO> getUserPage(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getNickname, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = new Page<>(page, Math.min(size, 100));
        IPage<User> result = userMapper.selectPage(userPage, wrapper);

        List<AdminUserVO> vos = result.getRecords().stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public AdminUserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toAdminUserVO(user);
    }

    @Override
    @Transactional
    @AdminLog(action = "UPDATE_USER_STATUS", targetType = "USER")
    public void updateUserStatus(Long adminId, Long userId, UserManageDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (user.getId().equals(adminId)) {
            throw BusinessException.badRequest("不能修改自己的状态");
        }

        user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        log.info("[用户管理] 管理员{}修改用户{}状态为{}", adminId, userId, dto.getStatus());
    }

    @Override
    @Transactional
    @AdminLog(action = "ASSIGN_USER_ROLES", targetType = "USER")
    public void assignUserRoles(Long adminId, Long userId, RoleAssignDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        // 删除用户现有角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        userRoleMapper.delete(wrapper);

        // 分配新角色
        for (Long roleId : dto.getRoleIds()) {
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw BusinessException.notFound("角色不存在: " + roleId);
            }
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(roleId)
                    .build();
            userRoleMapper.insert(userRole);
        }

        log.info("[用户管理] 管理员{}为用户{}分配角色: {}", adminId, userId, dto.getRoleIds());
    }

    @Override
    @Transactional
    public String resetUserPassword(Long adminId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("[用户管理] 管理员{}重置用户{}密码", adminId, userId);
        return newPassword;
    }

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // ════════════════════════ 帖子审核 ════════════════════════

    @Override
    public PageResult<PostVO> getPostPage(Integer status, String keyword, int page, int size) {
        Page<Post> postPage = new Page<>(page, Math.min(size, 100));
        IPage<Post> result = postMapper.selectPageWithAuthor(postPage, null, null, keyword, status, null, null, null);

        List<PostVO> vos = result.getRecords().stream()
                .map(post -> {
                    User author = userMapper.selectById(post.getAuthorId());
                    Category category = post.getCategoryId() != null ? categoryMapper.selectById(post.getCategoryId()) : null;
                    List<Tag> tags = tagMapper.selectByPostId(post.getId());
                    return toPostVO(post, author, category, tags);
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    @AdminLog(action = "AUDIT_POST", targetType = "POST")
    public void auditPost(Long adminId, Long postId, PostAuditDTO dto) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        post.setStatus(dto.getStatus());
        postMapper.updateById(post);

        log.info("[帖子审核] 管理员{}审核帖子{}状态为{}", adminId, postId, dto.getStatus());
    }

    @Override
    @Transactional
    @AdminLog(action = "DELETE_POST", targetType = "POST")
    public void forceDeletePost(Long adminId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        postMapper.deleteById(postId);
        log.info("[帖子管理] 管理员{}删除帖子{}", adminId, postId);
    }

    @Override
    @Transactional
    @AdminLog(action = "PIN_POST", targetType = "POST")
    public void pinPost(Long adminId, Long postId, boolean pinned, Integer untilDays) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        post.setIsPinned(pinned);
        if (pinned) {
            post.setPinnedAt(LocalDateTime.now());
            post.setPinnedUntil(untilDays != null ? LocalDateTime.now().plusDays(untilDays) : null);
        } else {
            post.setPinnedAt(null);
            post.setPinnedUntil(null);
        }
        postMapper.updateById(post);

        log.info("[帖子管理] 管理员{}置顶帖子{}: pinned={} untilDays={}", adminId, postId, pinned, untilDays);
    }

    @Override
    @Transactional
    @AdminLog(action = "FEATURE_POST", targetType = "POST")
    public void featurePost(Long adminId, Long postId, boolean featured, Integer untilDays) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }

        post.setIsFeatured(featured);
        if (featured) {
            post.setFeaturedAt(LocalDateTime.now());
            post.setFeaturedUntil(untilDays != null ? LocalDateTime.now().plusDays(untilDays) : null);
        } else {
            post.setFeaturedAt(null);
            post.setFeaturedUntil(null);
        }
        postMapper.updateById(post);

        log.info("[帖子管理] 管理员{}加精帖子{}: featured={} untilDays={}", adminId, postId, featured, untilDays);
    }

    // ════════════════════════ 评论审核 ════════════════════════

    @Override
    public PageResult<CommentVO> getCommentPage(Integer status, int page, int size) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }
        wrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> commentPage = new Page<>(page, Math.min(size, 100));
        IPage<Comment> result = commentMapper.selectPage(commentPage, wrapper);

        List<CommentVO> vos = result.getRecords().stream()
                .map(c -> {
                    User user = userMapper.selectById(c.getUserId());
                    User replyTo = c.getReplyToId() != null ? userMapper.selectById(c.getReplyToId()) : null;
                    return CommentVO.builder()
                            .id(c.getId())
                            .postId(c.getPostId())
                            .user(toUserVO(user))
                            .parentId(c.getParentId())
                            .replyTo(toUserVO(replyTo))
                            .content(c.getContent())
                            .likeCount(c.getLikeCount())
                            .createTime(c.getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    @AdminLog(action = "DELETE_COMMENT", targetType = "COMMENT")
    public void deleteComment(Long adminId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }

        commentMapper.deleteById(commentId);
        log.info("[评论管理] 管理员{}删除评论{}", adminId, commentId);
    }

    // ════════════════════════ 举报处理 ════════════════════════

    @Override
    public PageResult<ReportVO> getReportPage(Integer status, String targetType, int page, int size) {
        Page<Report> reportPage = new Page<>(page, Math.min(size, 100));
        IPage<Report> result = reportMapper.selectPageWithReporter(reportPage, status, targetType);

        List<ReportVO> vos = result.getRecords().stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public ReportVO getReportDetail(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw BusinessException.notFound("举报不存在");
        }
        return toReportVO(report);
    }

    @Override
    @Transactional
    @AdminLog(action = "HANDLE_REPORT", targetType = "REPORT")
    public void handleReport(Long adminId, Long reportId, ReportHandleDTO dto) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw BusinessException.notFound("举报不存在");
        }
        if (report.getStatus() != 1) {
            throw BusinessException.badRequest("该举报已处理");
        }

        report.setStatus(dto.getStatus());
        report.setHandlerId(adminId);
        report.setHandleResult(dto.getHandleResult());
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);

        // 通知举报者
        String statusLabel = dto.getStatus() == 2 ? "已处理" : "已驳回";
        String notifyContent = "你提交的举报（" + report.getReason() + "）已被管理员" + statusLabel
                + (dto.getHandleResult() != null && !dto.getHandleResult().isBlank()
                        ? "：" + dto.getHandleResult() : "。");
        notificationService.create(com.mindtalk.forum.modules.message.entity.Notification.builder()
                .userId(report.getReporterId())
                .notifyType(Constants.NOTIFY_SYSTEM)
                .title("举报处理结果")
                .content(notifyContent)
                .targetType("REPORT")
                .targetId(reportId)
                .isRead(false)
                .build());

        log.info("[举报处理] 管理员{}处理举报{}: status={}", adminId, reportId, dto.getStatus());
    }

    @Override
    @Transactional
    public void createReport(Long userId, CreateReportDTO dto) {
        if (!dictService.isValidKey("REPORT_TARGET_TYPE", dto.getTargetType())) {
            throw BusinessException.badRequest("无效的举报目标类型");
        }
        if (!dictService.isValidKey("REPORT_REASON", dto.getReason())) {
            throw BusinessException.badRequest("无效的举报原因");
        }

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, userId)
                .eq(Report::getTargetType, dto.getTargetType())
                .eq(Report::getTargetId, dto.getTargetId())
                .eq(Report::getStatus, 1);
        if (reportMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("您已举报过该内容，请勿重复举报");
        }

        Report report = Report.builder()
                .reporterId(userId)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status(1)
                .build();
        reportMapper.insert(report);
        log.info("[举报] 用户{}举报{}:{} status=待处理", userId, dto.getTargetType(), dto.getTargetId());
    }

    // ════════════════════════ 权限管理 ════════════════════════

    @Override
    public List<RoleVO> getAllRoles() {
        List<Role> roles = roleMapper.selectList(null);
        return roles.stream()
                .map(r -> RoleVO.builder()
                        .id(r.getId())
                        .roleName(r.getRoleName())
                        .roleCode(r.getRoleCode())
                        .description(r.getDescription())
                        .sortOrder(r.getSortOrder())
                        .status(r.getStatus())
                        .createTime(r.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionVO> getRolePermissions(Long roleId) {
        List<Permission> permissions = permissionMapper.selectByRoleId(roleId);
        return permissions.stream()
                .map(this::toPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AdminLog(action = "UPDATE_ROLE_PERMISSIONS", targetType = "ROLE")
    public void updateRolePermissions(Long adminId, Long roleId, PermissionUpdateDTO dto) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }

        rolePermissionMapper.deleteByRoleId(roleId);

        if (!dto.getPermissionIds().isEmpty()) {
            List<RolePermission> list = dto.getPermissionIds().stream()
                    .map(permId -> RolePermission.builder()
                            .roleId(roleId)
                            .permissionId(permId)
                            .build())
                    .collect(Collectors.toList());
            rolePermissionMapper.batchInsert(list);
        }

        log.info("[权限管理] 管理员{}更新角色{}权限: {}", adminId, roleId, dto.getPermissionIds());
    }

    @Override
    public List<PermissionTreeVO> getPermissionTree() {
        List<Permission> all = permissionMapper.selectList(null);
        Map<Long, List<Permission>> childrenMap = all.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() > 0)
                .collect(Collectors.groupingBy(Permission::getParentId));

        return all.stream()
                .filter(p -> p.getParentId() == null || p.getParentId() == 0)
                .map(p -> buildTreeNode(p, childrenMap))
                .collect(Collectors.toList());
    }

    private PermissionTreeVO buildTreeNode(Permission p, Map<Long, List<Permission>> childrenMap) {
        List<PermissionTreeVO> children = childrenMap.getOrDefault(p.getId(), Collections.emptyList())
                .stream()
                .map(child -> buildTreeNode(child, childrenMap))
                .collect(Collectors.toList());

        return PermissionTreeVO.builder()
                .id(p.getId())
                .parentId(p.getParentId())
                .permName(p.getPermName())
                .permCode(p.getPermCode())
                .permType(p.getPermType())
                .path(p.getPath())
                .icon(p.getIcon())
                .sortOrder(p.getSortOrder())
                .children(children)
                .build();
    }

    // ════════════════════════ 统计分析 ════════════════════════

    @Override
    public StatsOverviewVO getStatsOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        Long totalUsers = userMapper.selectCount(null);
        Long totalPosts = postMapper.selectCount(null);
        Long totalComments = commentMapper.selectCount(null);
        Long totalReports = reportMapper.selectCount(null);
        Long pendingReports = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, 1));

        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));
        Long todayNewPosts = postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreateTime, todayStart));
        Long todayNewComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().ge(Comment::getCreateTime, todayStart));

        return StatsOverviewVO.builder()
                .totalUsers(totalUsers)
                .totalPosts(totalPosts)
                .totalComments(totalComments)
                .totalReports(totalReports)
                .todayNewUsers(todayNewUsers)
                .todayNewPosts(todayNewPosts)
                .todayNewComments(todayNewComments)
                .pendingReports(pendingReports)
                .build();
    }

    @Override
    public List<StatsTrendVO> getStatsTrends(int days) {
        List<StatsTrendVO> trends = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            Long newUsers = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getCreateTime, dayStart)
                            .lt(User::getCreateTime, dayEnd));
            Long newPosts = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .ge(Post::getCreateTime, dayStart)
                            .lt(Post::getCreateTime, dayEnd));
            Long newComments = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .ge(Comment::getCreateTime, dayStart)
                            .lt(Comment::getCreateTime, dayEnd));

            trends.add(StatsTrendVO.builder()
                    .date(date.format(fmt))
                    .newUsers(newUsers)
                    .newPosts(newPosts)
                    .newComments(newComments)
                    .build());
        }

        return trends;
    }

    // ════════════════════════ 内部方法 ════════════════════════

    private AdminUserVO toAdminUserVO(User user) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<String> roleCodes = userRoles.stream()
                .map(ur -> {
                    Role role = roleMapper.selectById(ur.getRoleId());
                    return role != null ? role.getRoleCode() : "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .roles(roleCodes)
                .createTime(user.getCreateTime())
                .build();
    }

    private ReportVO toReportVO(Report report) {
        User reporter = userMapper.selectById(report.getReporterId());
        User handler = report.getHandlerId() != null ? userMapper.selectById(report.getHandlerId()) : null;

        return ReportVO.builder()
                .id(report.getId())
                .reporter(toUserVO(reporter))
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .handler(toUserVO(handler))
                .handleResult(report.getHandleResult())
                .handleTime(report.getHandleTime())
                .createTime(report.getCreateTime())
                .build();
    }

    private PostVO toPostVO(Post post, User author, Category category, List<Tag> tags) {
        return PostVO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .summary(post.getContentText() != null && post.getContentText().length() > 200
                        ? post.getContentText().substring(0, 200) + "..."
                        : post.getContentText())
                .author(toUserVO(author))
                .category(category != null ? com.mindtalk.forum.modules.post.vo.CategoryVO.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .icon(category.getIcon())
                        .sortOrder(category.getSortOrder())
                        .build() : null)
                .tags(tags.stream()
                        .map(t -> com.mindtalk.forum.modules.post.vo.TagVO.builder()
                                .id(t.getId())
                                .name(t.getName())
                                .build())
                        .collect(Collectors.toList()))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .isFeatured(post.getIsFeatured())
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .build();
    }

    private UserVO toUserVO(User user) {
        if (user == null) return null;
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private PermissionVO toPermissionVO(Permission p) {
        return PermissionVO.builder()
                .id(p.getId())
                .parentId(p.getParentId())
                .permName(p.getPermName())
                .permCode(p.getPermCode())
                .permType(p.getPermType())
                .path(p.getPath())
                .icon(p.getIcon())
                .sortOrder(p.getSortOrder())
                .build();
    }

    @Override
    public PageResult<AdminLogVO> getLogs(int page, int size, Long adminId, String action) {
        LambdaQueryWrapper<com.mindtalk.forum.modules.admin.entity.AdminLog> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) wrapper.eq(com.mindtalk.forum.modules.admin.entity.AdminLog::getAdminId, adminId);
        if (action != null && !action.isEmpty()) wrapper.eq(com.mindtalk.forum.modules.admin.entity.AdminLog::getAction, action);
        wrapper.orderByDesc(com.mindtalk.forum.modules.admin.entity.AdminLog::getCreateTime);

        IPage<com.mindtalk.forum.modules.admin.entity.AdminLog> result = adminLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<Long> adminIds = result.getRecords().stream().map(com.mindtalk.forum.modules.admin.entity.AdminLog::getAdminId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = adminIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(adminIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<AdminLogVO> vos = result.getRecords().stream().map(log -> {
            User u = userMap.get(log.getAdminId());
            return AdminLogVO.builder()
                    .id(log.getId()).adminId(log.getAdminId())
                    .adminName(u != null ? u.getNickname() : "未知")
                    .action(log.getAction()).targetType(log.getTargetType())
                    .targetId(log.getTargetId()).detail(log.getDetail())
                    .ip(log.getIp()).createTime(log.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void batchDeletePosts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            Post post = postMapper.selectById(id);
            if (post != null) postMapper.deleteById(id);
        }
        log.info("[批量操作] 删除帖子 count={}", ids.size());
    }

    @Override
    @Transactional
    public void batchDeleteComments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        commentMapper.deleteBatchIds(ids);
        log.info("[批量操作] 删除评论 count={}", ids.size());
    }

    @Override
    @Transactional
    public void batchUpdateUserStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            User user = userMapper.selectById(id);
            if (user != null) {
                user.setStatus(status);
                userMapper.updateById(user);
            }
        }
        log.info("[批量操作] 更新用户状态 count={} status={}", ids.size(), status);
    }

    @Override
    public List<Map<String, Object>> getCategoryDistribution() {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1).isNotNull(Post::getCategoryId);
        List<Post> posts = postMapper.selectList(wrapper);
        Map<Long, Long> countMap = posts.stream()
                .collect(Collectors.groupingBy(Post::getCategoryId, Collectors.counting()));
        List<Long> catIds = new ArrayList<>(countMap.keySet());
        Map<Long, Category> catMap = catIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(catIds).stream().collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        return countMap.entrySet().stream().map(e -> {
            Category cat = catMap.get(e.getKey());
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("name", cat != null ? cat.getName() : "未分类");
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getHourlyActivity() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("hour", h);
            m.put("count", 0);
            result.add(m);
        }
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1).apply("create_time >= NOW() - INTERVAL '30 days'");
        List<Post> posts = postMapper.selectList(wrapper);
        for (Post p : posts) {
            int hour = p.getCreateTime().getHour();
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) result.get(hour);
            m.put("count", ((Number) m.get("count")).intValue() + 1);
        }
        return result;
    }
}
