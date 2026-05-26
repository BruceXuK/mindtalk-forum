<template>
  <aside class="sidebar">
    <!-- Navigation -->
    <nav class="sidebar-nav">
      <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
        <el-icon :size="18"><HomeFilled /></el-icon>
        <span>首页</span>
      </router-link>
      <router-link to="/posts" class="nav-item" :class="{ active: route.path === '/posts' }">
        <el-icon :size="18"><Document /></el-icon>
        <span>全部帖子</span>
      </router-link>
      <router-link to="/search" class="nav-item" :class="{ active: route.path === '/search' }">
        <el-icon :size="18"><Search /></el-icon>
        <span>搜索</span>
      </router-link>
    </nav>

    <!-- 分类 -->
    <div class="sidebar-section">
      <div class="section-label">分类</div>
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="nav-item nav-item--sub nav-item--with-action"
      >
        <router-link :to="`/posts?categoryId=${cat.id}`" class="nav-item__link">
          <span class="cat-icon">{{ cat.icon || '📁' }}</span>
          <span class="cat-name">{{ cat.name }}</span>
        </router-link>
        <button
          v-if="userStore.isLoggedIn"
          class="sub-btn"
          :class="{ subscribed: subscribedCatIds.includes(cat.id) }"
          @click.stop="toggleCategory(cat.id)"
          :title="subscribedCatIds.includes(cat.id) ? '取消订阅' : '订阅分类'"
        >
          <el-icon :size="12"><StarFilled v-if="subscribedCatIds.includes(cat.id)" /><Star v-else /></el-icon>
        </button>
      </div>
    </div>

    <!-- 标签 -->
    <div class="sidebar-section">
      <div class="section-label">热门标签</div>
      <div class="tag-cloud">
        <router-link
          v-for="tag in tags"
          :key="tag.id"
          :to="`/posts?tagId=${tag.id}`"
          class="tag-item"
        >
          {{ tag.name }}
        </router-link>
      </div>
    </div>

    <div class="sidebar-section">
      <div class="section-label">订阅</div>
      <a :href="config.rss.path" target="_blank" class="nav-item nav-item--sub">
        <span>📡 RSS 订阅</span>
      </a>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { postApi } from '@/api/modules/post'
import { subscriptionApi } from '@/api/modules/subscription'
import { useUserStore } from '@/stores/modules/user'
import { useAppConfig } from '@/composables/useAppConfig'
import type { CategoryVO, TagVO } from '@/types'
import { HomeFilled, Document, Search, Star, StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const config = useAppConfig()
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const subscribedCatIds = ref<number[]>([])

onMounted(async () => {
  try {
    const [catRes, tagRes] = await Promise.all([postApi.getCategories(), postApi.getTags()])
    categories.value = (catRes.data as CategoryVO[]).slice(0, config.pagination.categoryLimit)
    tags.value = (tagRes.data as TagVO[]).slice(0, config.pagination.tagLimit)
    if (userStore.isLoggedIn) loadSubscriptions()
  } catch { /* 侧边栏加载失败不影响主体 */ }
})

async function loadSubscriptions() {
  try {
    const res = await subscriptionApi.getMyCategoryIds()
    subscribedCatIds.value = res.data || []
  } catch { /* ignore */ }
}

async function toggleCategory(catId: number) {
  try {
    if (subscribedCatIds.value.includes(catId)) {
      await subscriptionApi.unsubscribeCategory(catId)
      subscribedCatIds.value = subscribedCatIds.value.filter(id => id !== catId)
    } else {
      await subscriptionApi.subscribeCategory(catId)
      subscribedCatIds.value.push(catId)
    }
  } catch { /* ignore */ }
}
</script>

<style lang="scss" scoped>
.sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--module-gap));
  width: var(--sidebar-width);
  max-height: calc(100vh - var(--header-height) - var(--module-gap) * 2);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding-top: var(--spacing-xs);
}

// ── Nav ──
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 7px var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
  }

  &.active {
    background: var(--color-primary-50);
    color: var(--color-primary);
    font-weight: var(--font-weight-medium);
  }

  &--sub {
    padding: 5px var(--spacing-md);
  }
}

// ── Section ──
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.section-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  padding: 0 var(--spacing-md);
  margin-bottom: var(--spacing-xs);
}

.cat-icon {
  flex-shrink: 0;
  font-size: 13px;
}

.cat-name {
  flex: 1;
}

.nav-item--with-action {
  display: flex;
  align-items: center;
  padding: 0;

  .nav-item__link {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    flex: 1;
    padding: 5px var(--spacing-md);
    min-width: 0;
  }

  .sub-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border: none;
    background: none;
    color: var(--color-text-tertiary);
    cursor: pointer;
    border-radius: 4px;
    margin-right: 4px;
    flex-shrink: 0;
    transition: all var(--transition-fast);

    &:hover { color: var(--color-warning); background: var(--color-bg-secondary); }
    &.subscribed { color: #F59E0B; }
  }
}

// ── Tag Cloud ──
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 var(--spacing-md);
}

.tag-item {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-bg-secondary);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
    background: var(--color-primary-50);
  }
}

@media (max-width: 1023px) {
  .sidebar { display: none; }
}
</style>
