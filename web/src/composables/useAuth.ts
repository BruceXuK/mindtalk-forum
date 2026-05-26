import { useUserStore } from '@/stores/modules/user'
import { useRouter } from 'vue-router'

export function useAuth() {
  const userStore = useUserStore()
  const router = useRouter()

  const isAuthenticated = () => userStore.isLoggedIn

  const hasRole = (role: string) => userStore.hasRole(role)

  const login = (accessToken: string, refreshToken: string, roles: string[]) => {
    userStore.setToken(accessToken, refreshToken)
    sessionStorage.setItem('user_roles', JSON.stringify(roles))
  }

  const logout = () => {
    userStore.logout()
    router.push('/')
  }

  return { isAuthenticated, hasRole, login, logout }
}
