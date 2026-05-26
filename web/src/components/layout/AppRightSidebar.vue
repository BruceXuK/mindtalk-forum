<template>
  <aside class="right-sidebar">
    <!-- 我的关注（仅登录后显示） -->
    <div v-if="userStore.isLoggedIn" class="sidebar-section">
      <div class="section-label">我的关注</div>
      <div class="follow-search">
        <el-input
          v-model="followingKeyword"
          placeholder="搜索关注..."
          size="small"
          clearable
          class="follow-search-input"
        >
          <template #prefix>
            <el-icon :size="14"><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="follow-list">
        <router-link
          v-for="u in followingUsers"
          :key="u.id"
          :to="`/users/${u.id}`"
          class="follow-item"
        >
          <el-avatar :size="28" :src="u.avatarUrl" class="follow-avatar">
            {{ u.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="follow-info">
            <span class="follow-name">{{ u.nickname }}</span>
            <span class="follow-counts">{{ (u.followingCount || 0) }} 关注 · {{ (u.followerCount || 0) }} 粉丝</span>
          </div>
        </router-link>
        <div v-if="followingUsers.length === 0 && !followingLoading" class="empty-hint">
          {{ followingKeyword ? '未找到匹配的用户' : '暂未关注任何人' }}
        </div>
      </div>
    </div>

    <!-- 热门帖子 - 弱化 -->
    <div class="sidebar-section">
      <div class="section-label">热门推荐</div>
      <div class="hot-list">
        <router-link
          v-for="post in hotPosts"
          :key="post.id"
          :to="`/posts/${post.id}`"
          class="hot-item"
        >
          <span class="hot-title">{{ post.title }}</span>
        </router-link>
        <div v-if="hotPosts.length === 0" class="empty-hint">暂无热门内容</div>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="sidebar-footer">
      <template v-for="(link, idx) in config.footer.links" :key="link.label">
        <span v-if="idx > 0" class="footer-dot">·</span>
        <router-link :to="link.to" class="footer-link">{{ link.label }}</router-link>
      </template>
      <p class="footer-copy">{{ config.footer.copyright }}</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { postApi } from '@/api/modules/post'
import { userApi } from '@/api/modules/user'
import { useUserStore } from '@/stores/modules/user'
import { useAppConfig } from '@/composables/useAppConfig'
import type { PostVO, UserVO } from '@/types'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const config = useAppConfig()
const hotPosts = ref<PostVO[]>([])
const followingUsers = ref<UserVO[]>([])
const followingKeyword = ref('')
const followingLoading = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | null = null

function loadFollowing(keyword?: string) {
  if (!userStore.isLoggedIn) return
  followingLoading.value = true
  userApi.getFollowing({ keyword: keyword || undefined, size: config.pagination.followingSize })
    .then(res => { followingUsers.value = res.data || [] })
    .catch(() => { followingUsers.value = [] })
    .finally(() => { followingLoading.value = false })
}

watch(followingKeyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadFollowing(val), config.polling.mentionDebounce + 100)
})

watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) loadFollowing()
})

onMounted(async () => {
  if (userStore.isLoggedIn) loadFollowing()
  try {
    const res = await postApi.getHotPosts(config.pagination.hotPostsLimit)
    hotPosts.value = (res.data as PostVO[]) || []
  } catch { /* 右侧栏加载失败不影响主体 */ }
})
</script>

<style lang="scss" scoped>
.right-sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--module-gap));
  width: var(--right-sidebar-width);
  max-height: calc(100vh - var(--header-height) - var(--module-gap) * 2);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding-top: var(--spacing-xs);
}

.sidebar-section {
  // No card wrapper — just text
}

.section-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: var(--spacing-md);
}

.hot-list {
  display: flex;
  flex-direction: column;
}

.hot-item {
  display: block;
  padding: 6px 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-normal);
  transition: color var(--transition-fast);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;

  &:hover {
    color: var(--color-primary);
  }
}

.empty-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  padding: var(--spacing-sm) 0;
}

// ── Following Section ──
.follow-search {
  margin-bottom: var(--spacing-sm);
}

.follow-search-input {
  --el-input-bg-color: var(--color-bg-secondary);
  --el-input-border-color: transparent;
  --el-input-hover-border-color: var(--color-border);
  --el-input-focus-border-color: var(--color-primary);
}

.follow-list {
  display: flex;
  flex-direction: column;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 5px 0;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);

  &:hover {
    background: var(--color-bg-secondary);
  }
}

.follow-avatar {
  flex-shrink: 0;
}

.follow-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.follow-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-counts {
  font-size: 11px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

// ── Footer ──
.sidebar-footer {
  margin-top: auto;
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-divider);
}

.footer-link {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  transition: color var(--transition-fast);

  &:hover { color: var(--color-text-secondary); }
}

.footer-dot {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin: 0 4px;
}

.footer-copy {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-top: var(--spacing-sm);
}

@media (max-width: 1279px) {
  .right-sidebar { display: none; }
}
</style>
