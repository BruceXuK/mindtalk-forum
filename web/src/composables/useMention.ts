import { ref } from 'vue'
import { userApi } from '@/api/modules/user'
import { useAppConfig } from '@/composables/useAppConfig'
import type { UserVO } from '@/types'

/**
 * @提及用户 Composable
 * 监听 textarea 输入，触发 @ 时弹出用户搜索建议
 */
export function useMention() {
  const config = useAppConfig()
  const mentionVisible = ref(false)
  const mentionUsers = ref<UserVO[]>([])
  const mentionLoading = ref(false)
  const mentionKeyword = ref('')
  const mentionPos = ref({ top: 0, left: 0 })

  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 从光标位置向前解析 @keyword
   * 返回 { keyword, startPos }
   */
  function parseMentionQuery(textarea: HTMLTextAreaElement): { keyword: string; startPos: number } | null {
    const pos = textarea.selectionEnd
    const text = textarea.value
    // 从光标向前查找最近的 @
    let atPos = -1
    for (let i = pos - 1; i >= 0; i--) {
      if (text[i] === '@') {
        // @ 前面必须是空白、行首或标点
        if (i === 0 || /[\s,.;:!?，。；：！？、'")\]}>]/.test(text[i - 1])) {
          atPos = i
          break
        }
      }
      // 遇到空格或换行就停止
      if (text[i] === ' ' || text[i] === '\n') break
    }

    if (atPos === -1) return null

    const keyword = text.substring(atPos + 1, pos)
    // 关键词中不应包含空格或特殊分隔符
    if (/[\s@]/.test(keyword)) return null

    return { keyword, startPos: atPos }
  }

  /**
   * 搜索用户
   */
  async function searchUsers(keyword: string) {
    if (!keyword || keyword.length < 1) {
      mentionUsers.value = []
      mentionVisible.value = false
      return
    }
    mentionLoading.value = true
    try {
      const res = await userApi.searchUsers(keyword, config.pagination.mentionLimit)
      mentionUsers.value = res.data
      mentionVisible.value = res.data.length > 0
    } catch {
      mentionVisible.value = false
    } finally {
      mentionLoading.value = false
    }
  }

  /**
   * 在 textarea 上触发时调用
   */
  function onInput(e: Event) {
    const textarea = e.target as HTMLTextAreaElement
    const parsed = parseMentionQuery(textarea)

    if (!parsed) {
      mentionVisible.value = false
      mentionKeyword.value = ''
      return
    }

    mentionKeyword.value = parsed.keyword

    // 计算弹出位置
    const rect = textarea.getBoundingClientRect()
    mentionPos.value = {
      top: rect.top - 240,
      left: rect.left + 10,
    }

    // 防抖搜索
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => searchUsers(parsed.keyword), config.polling.mentionDebounce)
  }

  /**
   * 选中用户后，替换 textarea 中的 @keyword 为 @username
   */
  function selectUser(user: UserVO, textarea: HTMLTextAreaElement): number {
    const parsed = parseMentionQuery(textarea)
    if (!parsed) return 0

    const before = textarea.value.substring(0, parsed.startPos)
    const after = textarea.value.substring(textarea.selectionEnd)
    const mentionText = `@${user.username} `
    textarea.value = before + mentionText + after

    // 光标放到插入文本后
    const newPos = parsed.startPos + mentionText.length
    textarea.selectionStart = newPos
    textarea.selectionEnd = newPos
    textarea.focus()

    mentionVisible.value = false
    return user.id
  }

  function closeMention() {
    mentionVisible.value = false
  }

  return {
    mentionVisible,
    mentionUsers,
    mentionLoading,
    mentionKeyword,
    mentionPos,
    onInput,
    selectUser,
    closeMention,
  }
}

/**
 * 从文本中解析所有被 @username 提及的用户名列表
 */
export function parseMentionedUsernames(text: string): string[] {
  const regex = /(?<=^|[\s,.;:!?，。；：！？、'")\]}>])@([a-zA-Z0-9_一-鿿]+)/g
  const usernames = new Set<string>()
  let match: RegExpExecArray | null
  while ((match = regex.exec(text)) !== null) {
    usernames.add(match[1])
  }
  return Array.from(usernames)
}
