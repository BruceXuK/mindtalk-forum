import request from '../request'
import type { Result, PageResult, AdminUserVO, AdminLogVO, PostVO, CommentVO, ReportVO, RoleVO, PermissionVO, PermissionTreeVO, StatsOverviewVO, StatsTrendVO } from '@/types'

export const adminApi = {
  // 用户管理
  getUserList(params: { keyword?: string; status?: number; page: number; size: number }) {
    return request.get<any, Result<PageResult<AdminUserVO>>>('/admin/users', { params })
  },

  getUserDetail(id: number) {
    return request.get<any, Result<AdminUserVO>>(`/admin/users/${id}`)
  },

  updateUserStatus(id: number, data: { status: number }) {
    return request.put<any, Result<void>>(`/admin/users/${id}/status`, data)
  },

  assignUserRoles(id: number, data: { roleIds: number[] }) {
    return request.put<any, Result<void>>(`/admin/users/${id}/roles`, data)
  },

  resetUserPassword(id: number) {
    return request.post<any, Result<{ password: string }>>(`/admin/users/${id}/reset-password`)
  },

  // 帖子审核
  getPostList(params: { status?: number; keyword?: string; page: number; size: number }) {
    return request.get<any, Result<PageResult<PostVO>>>('/admin/posts', { params })
  },

  auditPost(id: number, data: { status: number; rejectReason?: string }) {
    return request.put<any, Result<void>>(`/admin/posts/${id}/status`, data)
  },

  deletePost(id: number) {
    return request.delete<any, Result<void>>(`/admin/posts/${id}`)
  },

  pinPost(id: number, pinned: boolean, untilDays?: number) {
    const query = untilDays != null ? `pinned=${pinned}&untilDays=${untilDays}` : `pinned=${pinned}`
    return request.put<any, Result<void>>(`/admin/posts/${id}/pin?${query}`)
  },

  featurePost(id: number, featured: boolean, untilDays?: number) {
    const query = untilDays != null ? `featured=${featured}&untilDays=${untilDays}` : `featured=${featured}`
    return request.put<any, Result<void>>(`/admin/posts/${id}/feature?${query}`)
  },

  // 评论审核
  getCommentList(params: { status?: number; page: number; size: number }) {
    return request.get<any, Result<PageResult<CommentVO>>>('/admin/comments', { params })
  },

  deleteComment(id: number) {
    return request.delete<any, Result<void>>(`/admin/comments/${id}`)
  },

  // 举报处理
  getReportList(params: { status?: number; targetType?: string; page: number; size: number }) {
    return request.get<any, Result<PageResult<ReportVO>>>('/admin/reports', { params })
  },

  getReportDetail(id: number) {
    return request.get<any, Result<ReportVO>>(`/admin/reports/${id}`)
  },

  handleReport(id: number, data: { status: number; handleResult: string }) {
    return request.put<any, Result<void>>(`/admin/reports/${id}/handle`, data)
  },

  // 权限管理
  getRoleList() {
    return request.get<any, Result<RoleVO[]>>('/admin/roles')
  },

  getRolePermissions(roleId: number) {
    return request.get<any, Result<PermissionVO[]>>(`/admin/roles/${roleId}/permissions`)
  },

  updateRolePermissions(roleId: number, data: { permissionIds: number[] }) {
    return request.put<any, Result<void>>(`/admin/roles/${roleId}/permissions`, data)
  },

  getPermissionTree() {
    return request.get<any, Result<PermissionTreeVO[]>>('/admin/roles/permissions/tree')
  },

  // 统计分析
  getStatsOverview() {
    return request.get<any, Result<StatsOverviewVO>>('/admin/stats/overview')
  },

  getStatsTrends(days: number = 7) {
    return request.get<any, Result<StatsTrendVO[]>>('/admin/stats/trends', { params: { days } })
  },

  getLogs(params: { page?: number; size?: number; adminId?: number; action?: string }) {
    return request.get<any, Result<PageResult<AdminLogVO>>>('/admin/logs', { params })
  },

  // 图表
  getCategoryDistribution() {
    return request.get<any, Result<any[]>>('/admin/stats/category-distribution')
  },
  getHourlyActivity() {
    return request.get<any, Result<any[]>>('/admin/stats/hourly-activity')
  },

  // 批量操作
  batchDeletePosts(ids: number[]) {
    return request.post<any, Result<void>>('/admin/posts/batch-delete', { ids })
  },
  batchDeleteComments(ids: number[]) {
    return request.post<any, Result<void>>('/admin/comments/batch-delete', { ids })
  },
  batchBanUsers(ids: number[]) {
    return request.post<any, Result<void>>('/admin/users/batch-ban', { ids })
  },
  batchUnbanUsers(ids: number[]) {
    return request.post<any, Result<void>>('/admin/users/batch-unban', { ids })
  }
}
