import request from '../request'
import type { Result, PageResult } from '@/types'

export const sensitiveWordApi = {
  getList(params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<any>>>('/admin/sensitive-words', { params })
  },
  add(word: string, replacement: string) {
    return request.post<any, Result<any>>('/admin/sensitive-words', { word, replacement })
  },
  delete(id: number) {
    return request.delete<any, Result<void>>(`/admin/sensitive-words/${id}`)
  }
}
