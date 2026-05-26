import request from '../request'
import type { Result, NotificationSettingVO } from '@/types'

export const notificationSettingsApi = {
  getList() {
    return request.get<any, Result<NotificationSettingVO[]>>('/notification-settings')
  },

  update(notifyType: string, enabled: boolean) {
    return request.put<any, Result<void>>('/notification-settings', { notifyType, enabled })
  }
}
