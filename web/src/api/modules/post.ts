import request from '../request'
import type { Result, PageResult, PostVO, CategoryVO, TagVO } from '@/types'

export interface CreatePostDTO {
  title: string
  content: string
  contentText: string
  categoryId: number
  tagIds: number[]
  status?: number   // 0-草稿 1-发布（默认）
  seriesId?: number
}

export const postApi = {
  getList(params: { page: number; size: number; categoryId?: number; tagId?: number; keyword?: string; status?: number; orderBy?: string; userId?: number }) {
    return request.get<any, Result<PageResult<PostVO>>>('/posts', { params })
  },

  getHotPosts(limit: number = 10) {
    return request.get<any, Result<PostVO[]>>('/posts/hot', { params: { limit } })
  },

  getFollowingFeed(params: { page: number; size: number; categoryId?: number; orderBy?: string }) {
    return request.get<any, Result<PageResult<PostVO>>>('/posts/following', { params })
  },

  getRecommended(limit: number = 10) {
    return request.get<any, Result<PostVO[]>>('/posts/recommended', { params: { limit } })
  },

  getRanking(period: string = 'weekly', limit: number = 20) {
    return request.get<any, Result<PostVO[]>>('/posts/ranking', { params: { period, limit } })
  },

  getSimilar(id: number, limit: number = 5) {
    return request.get<any, Result<PostVO[]>>(`/posts/${id}/similar`, { params: { limit } })
  },

  getDetail(id: number) {
    return request.get<any, Result<PostVO>>(`/posts/${id}`)
  },

  create(data: CreatePostDTO) {
    return request.post<any, Result<PostVO>>('/posts', data)
  },

  update(id: number, data: CreatePostDTO) {
    return request.put<any, Result<PostVO>>(`/posts/${id}`, data)
  },

  delete(id: number) {
    return request.delete<any, Result<void>>(`/posts/${id}`)
  },

  likePost(id: number) {
    return request.post<any, Result<void>>(`/posts/${id}/like`)
  },

  collectPost(id: number) {
    return request.post<any, Result<void>>(`/posts/${id}/collect`)
  },

  recordView(id: number) {
    return request.post<any, Result<void>>(`/posts/${id}/view`)
  },

  getCategories() {
    return request.get<any, Result<CategoryVO[]>>('/categories')
  },

  getTags() {
    return request.get<any, Result<TagVO[]>>('/tags')
  },

  getMyDrafts(params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<PostVO>>>('/posts/me/drafts', { params })
  },

  publishDraft(id: number) {
    return request.put<any, Result<PostVO>>(`/posts/${id}/publish`)
  }
}
