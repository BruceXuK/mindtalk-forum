import request from '../request'
import type { Result, AnnouncementVO } from '@/types'

export const announcementApi = {
  getActiveList() {
    return request.get<any, Result<AnnouncementVO[]>>('/announcements/active')
  }
}
