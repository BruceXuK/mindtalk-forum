import request from '../request'
import type { Result, BadgeVO } from '@/types'

export const badgeApi = {
  getMyBadges() {
    return request.get<any, Result<BadgeVO[]>>('/badges/my')
  },

  getUserBadges(userId: number) {
    return request.get<any, Result<BadgeVO[]>>(`/badges/user/${userId}`)
  }
}
