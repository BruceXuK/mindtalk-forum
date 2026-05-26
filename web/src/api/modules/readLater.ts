import request from '../request'
import type { Result, PageResult, ReadLaterVO } from '@/types'

export const readLaterApi = {
  getList(params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<ReadLaterVO>>>('/read-later', { params })
  },

  add(postId: number) {
    return request.post<any, Result<void>>(`/read-later/${postId}`)
  },

  remove(postId: number) {
    return request.delete<any, Result<void>>(`/read-later/${postId}`)
  },

  checkStatus(postId: number) {
    return request.get<any, Result<{ bookmarked: boolean }>>(`/read-later/${postId}/status`)
  }
}
