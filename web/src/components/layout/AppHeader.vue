<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- 移动端：汉堡菜单 -->
      <button class="menu-toggle" @click="toggleMobileMenu" aria-label="菜单">
        <span class="menu-icon" :class="{ open: mobileMenuOpen }">
          <i></i><i></i><i></i>
        </span>
      </button>

      <!-- Logo -->
      <router-link to="/" class="logo">{{ config.logoText }}</router-link>

      <!-- 搜索 -->
      <div class="search-area" @click="openSearch">
        <el-icon :size="16"><Search /></el-icon>
        <span class="search-placeholder">{{ config.search.placeholder }}</span>
        <kbd>{{ config.hotkeys.search }}</kbd>
      </div>

      <!-- 右侧操作 -->
      <div class="header-actions">
        <!-- 主题切换 -->
        <button class="icon-btn theme-toggle" @click="toggleTheme" :title="isDark ? '切换亮色' : '切换暗色'">
          <el-icon :size="18"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </button>

        <template v-if="userStore.isLoggedIn">
          <!-- 发帖 -->
          <router-link to="/posts/create" class="create-btn">
            <el-icon :size="16"><EditPen /></el-icon>
            <span class="btn-text">发帖</span>
          </router-link>

          <!-- 消息 -->
          <router-link to="/messages" class="icon-btn" title="消息中心">
            <el-badge :value="userStore.unreadCount" :hidden="!userStore.unreadCount" :max="99">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </router-link>

          <!-- 用户头像下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl" class="avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="messages">
                  <el-icon><Bell /></el-icon> 消息中心
                </el-dropdown-item>
                <el-dropdown-item command="notifSettings">
                  <el-icon><Setting /></el-icon> 通知设置
                </el-dropdown-item>
                <el-dropdown-item command="create">
                  <el-icon><EditPen /></el-icon> 发布帖子
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.hasRole('ADMIN')" command="admin" divided>
                  <el-icon><Setting /></el-icon> 管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button type="primary" size="small" @click="router.push('/login')">登录</el-button>
          <el-button size="small" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { useTheme } from '@/composables/useTheme'
import { useSidebar } from '@/composables/useSidebar'
import { useAppConfig } from '@/composables/useAppConfig'
import {
  Search, EditPen, Bell, User, ArrowDown, Setting, SwitchButton, Sunny, Moon
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const config = useAppConfig()
const { isDark, toggleTheme } = useTheme()
const { mobileMenuOpen, toggleMobileMenu } = useSidebar()

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUnreadCount()
    pollTimer = setInterval(() => userStore.fetchUnreadCount(), config.polling.unreadCount)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const userInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.charAt(0)
})

function openSearch() {
  router.push('/search')
}

function handleCommand(cmd: string) {
  switch (cmd) {
    case 'profile': router.push('/profile'); break
    case 'messages': router.push('/messages'); break
    case 'notifSettings': router.push('/settings/notifications'); break
    case 'create': router.push('/posts/create'); break
    case 'admin': router.push('/admin'); break
    case 'logout': userStore.logout(); router.push('/'); break
  }
}
</script>

<style lang="scss" scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  height: var(--header-height);
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid var(--color-divider);
  backdrop-filter: blur(16px) saturate(180%);

  html.dark & {
    background: rgba(15, 23, 42, 0.8);
  }
}

.header-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  height: 100%;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.menu-toggle { display: none; background: none; border: none; cursor: pointer; padding: 4px; }
.menu-icon {
  display: flex; flex-direction: column; gap: 4px; width: 20px;
  i { display: block; height: 2px; background: var(--color-text-primary); border-radius: 1px; transition: var(--transition-fast); }
  &.open {
    i:nth-child(1) { transform: rotate(45deg) translate(4px, 4px); }
    i:nth-child(2) { opacity: 0; }
    i:nth-child(3) { transform: rotate(-45deg) translate(4px, -4px); }
  }
}

.logo {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: -0.3px;
}

.search-area {
  flex: 1;
  max-width: 420px;
  height: 38px;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 0 var(--spacing-md);
  background: var(--color-bg-secondary);
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--color-text-tertiary);

  &:hover {
    border-color: var(--color-border);
    background: var(--color-bg);
  }

  .search-placeholder { font-size: var(--font-size-sm); flex: 1; }
  kbd {
    font-size: var(--font-size-xs);
    padding: 1px 6px;
    background: var(--color-bg);
    border-radius: 4px;
    border: 1px solid var(--color-border);
    color: var(--color-text-tertiary);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  flex-shrink: 0;
}

.icon-btn {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: var(--transition-fast);
  &:hover { background: var(--color-bg-secondary); color: var(--color-text-primary); }
}

.create-btn {
  display: flex; align-items: center; gap: 4px;
  height: 34px; padding: 0 var(--spacing-md);
  background: var(--color-primary); color: #fff;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast);
  &:hover { background: var(--color-primary-hover); transform: translateY(-1px); box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25); }
}

.user-trigger {
  display: flex; align-items: center; gap: var(--spacing-sm);
  cursor: pointer; padding: 2px 4px; border-radius: var(--radius-sm);
  transition: var(--transition-fast);
  &:hover { background: var(--color-bg-secondary); }
  .avatar { flex-shrink: 0; }
  .username { font-size: var(--font-size-sm); color: var(--color-text-primary); }
}

@media (max-width: 1023px) {
  .menu-toggle { display: block; }
  .search-area { max-width: 200px; kbd { display: none; } }
  .btn-text { display: none; }
}

@media (max-width: 767px) {
  .header-inner { padding: 0 var(--spacing-md); }
  .search-area { max-width: none; flex: 0; width: 36px; justify-content: center;
    .search-placeholder, kbd { display: none; }
  }
  .username { display: none; }
}
</style>
