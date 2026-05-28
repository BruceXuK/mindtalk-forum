import request from '../request'
import type { Result } from '@/types'

export interface CreateReportParams {
  targetType: string
  targetId: number
  reason: string
  description?: string
}

export const reportApi = {
  createReport(data: CreateReportParams) {
    return request.post<any, Result<void>>('/reports', data)
  }
}
