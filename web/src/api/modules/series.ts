import request from '../request'
import type { Result, PageResult, SeriesVO, SeriesDetailVO, PostSeriesContextVO, CreateSeriesDTO, UpdateSeriesDTO } from '@/types'

export const seriesApi = {
  create(data: CreateSeriesDTO) {
    return request.post<any, Result<SeriesDetailVO>>('/series', data)
  },

  update(id: number, data: UpdateSeriesDTO) {
    return request.put<any, Result<SeriesDetailVO>>(`/series/${id}`, data)
  },

  delete(id: number) {
    return request.delete<any, Result<void>>(`/series/${id}`)
  },

  getDetail(id: number) {
    return request.get<any, Result<SeriesDetailVO>>(`/series/${id}`)
  },

  getMySeries() {
    return request.get<any, Result<SeriesVO[]>>('/series/my')
  },

  getUserSeries(userId: number, params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<SeriesVO>>>(`/series/user/${userId}`, { params })
  },

  addPost(seriesId: number, postId: number) {
    return request.post<any, Result<void>>(`/series/${seriesId}/posts`, { postId })
  },

  removePost(seriesId: number, postId: number) {
    return request.delete<any, Result<void>>(`/series/${seriesId}/posts/${postId}`)
  },

  getPostSeriesContext(postId: number) {
    return request.get<any, Result<PostSeriesContextVO | null>>(`/series/by-post/${postId}`)
  }
}
