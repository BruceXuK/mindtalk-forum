import request from '../request'
import type { Result } from '@/types'

export const subscriptionApi = {
  // Tags
  subscribeTag(tagId: number) {
    return request.post<any, Result<void>>(`/subscriptions/tags/${tagId}`)
  },
  unsubscribeTag(tagId: number) {
    return request.delete<any, Result<void>>(`/subscriptions/tags/${tagId}`)
  },
  checkTagStatus(tagId: number) {
    return request.get<any, Result<{ subscribed: boolean }>>(`/subscriptions/tags/${tagId}/status`)
  },
  getMyTagIds() {
    return request.get<any, Result<number[]>>('/subscriptions/tags/my')
  },

  // Categories
  subscribeCategory(categoryId: number) {
    return request.post<any, Result<void>>(`/subscriptions/categories/${categoryId}`)
  },
  unsubscribeCategory(categoryId: number) {
    return request.delete<any, Result<void>>(`/subscriptions/categories/${categoryId}`)
  },
  checkCategoryStatus(categoryId: number) {
    return request.get<any, Result<{ subscribed: boolean }>>(`/subscriptions/categories/${categoryId}/status`)
  },
  getMyCategoryIds() {
    return request.get<any, Result<number[]>>('/subscriptions/categories/my')
  }
}
