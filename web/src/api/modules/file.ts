import request from '../request'
import type { Result } from '@/types'

export const fileApi = {
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, Result<{ url: string; key: string }>>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
