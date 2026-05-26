import request from '../request'
import type { Result, PageResult, NotificationVO } from '@/types'

export const messageApi = {
  getList(params: { page: number; size: number }) {
    return request.get<any, Result<PageResult<NotificationVO>>>('/notifications', { params })
  },

  markAsRead(id: number) {
    return request.put<any, Result<void>>(`/notifications/${id}/read`)
  },

  markAllAsRead() {
    return request.put<any, Result<void>>('/notifications/read-all')
  },

  getUnreadCount() {
    return request.get<any, Result<number>>('/notifications/unread-count')
  }
}
