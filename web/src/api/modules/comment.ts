import request from '../request'
import type { Result, PageResult, CommentVO } from '@/types'

export const commentApi = {
  getList(params: { postId: number; page: number; size: number; orderBy?: string }) {
    return request.get<any, Result<PageResult<CommentVO>>>('/comments', { params })
  },

  create(data: { postId: number; content: string; parentId?: number; replyToId?: number; mentionedUserIds?: number[] }) {
    return request.post<any, Result<CommentVO>>('/comments', data)
  },

  getReplies(commentId: number) {
    return request.get<any, Result<CommentVO[]>>(`/comments/${commentId}/replies`)
  },

  like(commentId: number) {
    return request.post<any, Result<void>>(`/comments/${commentId}/like`)
  },

  delete(commentId: number) {
    return request.delete<any, Result<void>>(`/comments/${commentId}`)
  }
}
