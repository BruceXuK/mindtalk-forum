import request from '../request'
import type { Result, PageResult, ReadingHistoryVO } from '@/types'

export const readingHistoryApi = {
  getList(params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<ReadingHistoryVO>>>('/reading-history', { params })
  },

  record(postId: number) {
    return request.post<any, Result<void>>(`/reading-history/${postId}`)
  },

  delete(id: number) {
    return request.delete<any, Result<void>>(`/reading-history/${id}`)
  },

  clearAll() {
    return request.delete<any, Result<void>>('/reading-history')
  }
}
