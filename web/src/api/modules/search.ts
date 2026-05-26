import request from '../request'
import type { Result, PageResult, PostVO } from '@/types'

export const searchApi = {
  search(params: { keyword: string; page?: number; size?: number; categoryId?: number; dateFrom?: string; dateTo?: string }) {
    return request.get<any, Result<PageResult<PostVO>>>('/search', { params })
  },

  getSuggestions(keyword: string) {
    return request.get<any, Result<string[]>>('/search/suggest', { params: { keyword } })
  },

  getHotSearches() {
    return request.get<any, Result<string[]>>('/search/hot')
  }
}
