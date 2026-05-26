import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'

export function useHotkeys() {
  const router = useRouter()
  const userStore = useUserStore()

  function handler(e: KeyboardEvent) {
    const isInput = ['INPUT', 'TEXTAREA', 'SELECT'].includes((e.target as HTMLElement)?.tagName)
    const mod = e.ctrlKey || e.metaKey

    // Ctrl+K / Cmd+K -> search
    if (mod && e.key === 'k') {
      e.preventDefault()
      router.push('/search')
      return
    }

    // Ctrl+N / Cmd+N -> new post (requires login)
    if (mod && e.key === 'n') {
      e.preventDefault()
      if (userStore.isLoggedIn) {
        router.push('/posts/create')
      }
      return
    }

    // / -> focus search (only when not in input)
    if (!isInput && e.key === '/' && !mod) {
      e.preventDefault()
      router.push('/search')
    }
  }

  onMounted(() => document.addEventListener('keydown', handler))
  onUnmounted(() => document.removeEventListener('keydown', handler))
}
