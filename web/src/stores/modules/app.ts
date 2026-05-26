import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type Theme = 'light' | 'dark' | 'system'

export const useAppStore = defineStore('app', () => {
  const theme = ref<Theme>((localStorage.getItem('theme-preference') as Theme) || 'system')
  const sidebarCollapsed = ref(false)
  const mobileMenuOpen = ref(false)
  let mediaQuery: MediaQueryList | null = null

  function applyTheme(t: Theme) {
    const isDark = t === 'dark' || (t === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    document.documentElement.classList.toggle('dark', isDark)
  }

  function initTheme() {
    applyTheme(theme.value)
    if (theme.value === 'system') {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', () => applyTheme('system'))
    }
  }

  watch(theme, (val) => {
    localStorage.setItem('theme-preference', val)
    if (mediaQuery) { mediaQuery.removeEventListener('change', () => applyTheme('system')); mediaQuery = null }
    if (val === 'system') {
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', () => applyTheme('system'))
    }
    applyTheme(val)
  })

  function setTheme(t: Theme) { theme.value = t }
  function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value }
  function openMobileMenu() { mobileMenuOpen.value = true; document.body.style.overflow = 'hidden' }
  function closeMobileMenu() { mobileMenuOpen.value = false; document.body.style.overflow = '' }

  return { theme, sidebarCollapsed, mobileMenuOpen, initTheme, setTheme, toggleSidebar, openMobileMenu, closeMobileMenu }
})
