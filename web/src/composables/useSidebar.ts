import { computed } from 'vue'
import { useAppStore } from '@/stores/modules/app'

export function useSidebar() {
  const appStore = useAppStore()
  const mobileMenuOpen = computed(() => appStore.mobileMenuOpen)
  const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
  const toggleMobileMenu = () => {
    if (appStore.mobileMenuOpen) { appStore.closeMobileMenu() }
    else { appStore.openMobileMenu() }
  }
  const closeMobileMenu = () => appStore.closeMobileMenu()
  const toggleSidebar = () => appStore.toggleSidebar()
  return { mobileMenuOpen, sidebarCollapsed, toggleMobileMenu, closeMobileMenu, toggleSidebar }
}
