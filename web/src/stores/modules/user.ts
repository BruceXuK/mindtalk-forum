import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserVO } from '@/types'
import { userApi } from '@/api/modules/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(sessionStorage.getItem('access_token') || '')
  const refreshToken = ref<string>(sessionStorage.getItem('refresh_token') || '')
  const userInfo = ref<UserVO | null>(null)
  const isLoggedIn = ref(!!token.value)

  // 初始化时从 sessionStorage 恢复用户信息
  const savedUserInfo = sessionStorage.getItem('user_info')
  if (savedUserInfo) {
    try {
      userInfo.value = JSON.parse(savedUserInfo)
    } catch { /* ignore */ }
  }

  function setToken(accessToken: string, refreshTokenVal?: string) {
    token.value = accessToken
    sessionStorage.setItem('access_token', accessToken)
    if (refreshTokenVal) {
      refreshToken.value = refreshTokenVal
      sessionStorage.setItem('refresh_token', refreshTokenVal)
    }
    isLoggedIn.value = true
  }

  function setUserInfo(info: UserVO) {
    userInfo.value = info
    sessionStorage.setItem('user_info', JSON.stringify(info))
  }

  async function fetchUserInfo() {
    try {
      const res = await userApi.getCurrentUser()
      if (res.data) {
        setUserInfo(res.data)
      }
    } catch {
      // fetchUserInfo 失败不清理登录态，token 仍然有效
      console.warn('[userStore] fetchUserInfo 失败，保持现有登录态')
    }
  }

  function hasRole(role: string): boolean {
    if (!userInfo.value && !token.value) return false
    const roles = parseRoles()
    return roles.includes(role)
  }

  function parseRoles(): string[] {
    // 优先从 sessionStorage 读取
    const cached = JSON.parse(sessionStorage.getItem('user_roles') || '[]')
    if (cached.length > 0) return cached
    // 回退：从 JWT token 中解析 role claim
    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      const parsed = [payload.role || 'USER']
      sessionStorage.setItem('user_roles', JSON.stringify(parsed))
      return parsed
    } catch {
      return []
    }
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('refresh_token')
    sessionStorage.removeItem('user_info')
    sessionStorage.removeItem('user_roles')
  }

  return { token, refreshToken, userInfo, isLoggedIn, setToken, setUserInfo, fetchUserInfo, hasRole, logout }
})
