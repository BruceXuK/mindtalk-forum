import { computed } from 'vue'
import { useAppStore, type Theme } from '@/stores/modules/app'

export function useTheme() {
  const appStore = useAppStore()
  const isDark = computed(() => document.documentElement.classList.contains('dark'))
  const theme = computed(() => appStore.theme)
  const toggleTheme = () => {
    const next: Theme = appStore.theme === 'dark' ? 'light' : 'dark'
    appStore.setTheme(next)
  }
  return { isDark, theme, toggleTheme }
}
