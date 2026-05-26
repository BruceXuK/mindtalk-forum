import request from '../request'
import type { Result, LoginResultVO, UserVO, UserProfileVO } from '@/types'

export const userApi = {
  register(data: { username: string; password: string; email?: string; nickname?: string }) {
    return request.post<any, Result<UserVO>>('/auth/register', data)
  },

  login(data: { account: string; password: string }) {
    return request.post<any, Result<LoginResultVO>>('/auth/login', data)
  },

  refreshToken(data: { refreshToken: string }) {
    return request.post<any, Result<LoginResultVO>>('/auth/refresh', data)
  },

  logout() {
    return request.post<any, Result<void>>('/auth/logout')
  },

  getCurrentUser() {
    return request.get<any, Result<UserVO>>('/users/me')
  },

  updatePassword(data: { oldPassword: string; newPassword: string }) {
    return request.put<any, Result<void>>('/users/me/password', data)
  },

  updateProfile(data: { nickname?: string; bio?: string; gender?: number; birthday?: string; location?: string }) {
    return request.put<any, Result<UserVO>>('/users/me/profile', data)
  },

  uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, Result<UserVO>>('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  getUserProfile(userId: number) {
    return request.get<any, Result<UserProfileVO>>(`/users/${userId}/profile`)
  },

  followUser(userId: number) {
    return request.post<any, Result<void>>(`/users/${userId}/follow`)
  },

  unfollowUser(userId: number) {
    return request.delete<any, Result<void>>(`/users/${userId}/follow`)
  },

  getFollowing(params?: { keyword?: string; size?: number }) {
    return request.get<any, Result<UserVO[]>>('/users/me/following', { params })
  },

  searchUsers(keyword: string, limit: number = 10) {
    return request.get<any, Result<UserVO[]>>('/users/search', { params: { keyword, limit } })
  }
}
