package com.mindtalk.forum.modules.admin.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.dto.*;
import com.mindtalk.forum.modules.admin.vo.*;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.user.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface AdminService {

    // 用户管理
    PageResult<AdminUserVO> getUserPage(String keyword, Integer status, int page, int size);

    AdminUserVO getUserDetail(Long userId);

    void updateUserStatus(Long adminId, Long userId, UserManageDTO dto);

    void assignUserRoles(Long adminId, Long userId, RoleAssignDTO dto);

    String resetUserPassword(Long adminId, Long userId);

    // 帖子审核
    PageResult<PostVO> getPostPage(Integer status, String keyword, int page, int size);

    void auditPost(Long adminId, Long postId, PostAuditDTO dto);

    void forceDeletePost(Long adminId, Long postId);

    void pinPost(Long adminId, Long postId, boolean pinned, Integer untilDays);

    void featurePost(Long adminId, Long postId, boolean featured, Integer untilDays);

    // 评论审核
    PageResult<com.mindtalk.forum.modules.comment.vo.CommentVO> getCommentPage(Integer status, int page, int size);

    void deleteComment(Long adminId, Long commentId);

    // 举报处理
    PageResult<ReportVO> getReportPage(Integer status, String targetType, int page, int size);

    ReportVO getReportDetail(Long reportId);

    void handleReport(Long adminId, Long reportId, ReportHandleDTO dto);

    void createReport(Long userId, CreateReportDTO dto);

    // 权限管理
    List<RoleVO> getAllRoles();

    List<PermissionVO> getRolePermissions(Long roleId);

    void updateRolePermissions(Long adminId, Long roleId, PermissionUpdateDTO dto);

    List<PermissionTreeVO> getPermissionTree();

    // 统计分析
    StatsOverviewVO getStatsOverview();

    List<StatsTrendVO> getStatsTrends(int days);

    // 操作日志
    PageResult<AdminLogVO> getLogs(int page, int size, Long adminId, String action);

    // 批量操作
    void batchDeletePosts(List<Long> ids);
    void batchDeleteComments(List<Long> ids);
    void batchUpdateUserStatus(List<Long> ids, Integer status);

    List<Map<String, Object>> getCategoryDistribution();
    List<Map<String, Object>> getHourlyActivity();
}
