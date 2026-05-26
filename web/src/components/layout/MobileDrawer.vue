<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="mobileMenuOpen" class="drawer-overlay" @click="closeMobileMenu"></div>
    </Transition>
    <Transition name="slide-left">
      <div v-if="mobileMenuOpen" class="drawer">
        <div class="drawer-header">
          <span class="drawer-logo">MindTalk</span>
          <button class="drawer-close" @click="closeMobileMenu">
            <el-icon :size="20"><Close /></el-icon>
          </button>
        </div>
        <div class="drawer-body">
          <nav class="drawer-nav">
            <router-link to="/" class="drawer-item" @click="closeMobileMenu">
              <el-icon :size="18"><HomeFilled /></el-icon> 首页
            </router-link>
            <router-link to="/posts" class="drawer-item" @click="closeMobileMenu">
              <el-icon :size="18"><Document /></el-icon> 全部帖子
            </router-link>
            <router-link to="/search" class="drawer-item" @click="closeMobileMenu">
              <el-icon :size="18"><Search /></el-icon> 搜索
            </router-link>
          </nav>
          <div class="drawer-divider"></div>
          <div class="drawer-section-title">分类</div>
          <div class="drawer-categories">
            <router-link
              v-for="cat in categories"
              :key="cat.id"
              :to="`/posts?categoryId=${cat.id}`"
              class="drawer-item"
              @click="closeMobileMenu"
            >
              {{ cat.icon || '📁' }} {{ cat.name }}
            </router-link>
          </div>
          <div class="drawer-divider"></div>
          <div class="drawer-section-title">热门标签</div>
          <div class="drawer-tags">
            <router-link
              v-for="tag in tags"
              :key="tag.id"
              :to="`/posts?tagId=${tag.id}`"
              class="drawer-tag"
              @click="closeMobileMenu"
            >
              #{{ tag.name }}
            </router-link>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { postApi } from '@/api/modules/post'
import { useSidebar } from '@/composables/useSidebar'
import type { CategoryVO, TagVO } from '@/types'
import { Close, HomeFilled, Document, Search } from '@element-plus/icons-vue'

const { mobileMenuOpen, closeMobileMenu } = useSidebar()
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])

onMounted(async () => {
  try {
    const [catRes, tagRes] = await Promise.all([postApi.getCategories(), postApi.getTags()])
    categories.value = (catRes.data as CategoryVO[]).slice(0, 8)
    tags.value = (tagRes.data as TagVO[]).slice(0, 12)
  } catch { /* ignore */ }
})
</script>

<style lang="scss" scoped>
.drawer-overlay {
  position: fixed; inset: 0; z-index: var(--z-overlay);
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(2px);
}

.drawer {
  position: fixed; top: 0; left: 0; bottom: 0;
  z-index: var(--z-drawer);
  width: 280px; max-width: 85vw;
  background: var(--color-card);
  display: flex; flex-direction: column;
  box-shadow: var(--shadow-xl);
}

.drawer-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: var(--border-width) solid var(--color-border);
}
.drawer-logo { font-size: var(--font-size-lg); font-weight: var(--font-weight-bold); color: var(--color-primary); }
.drawer-close { background: none; border: none; cursor: pointer; color: var(--color-text-secondary); padding: 4px; }

.drawer-body {
  flex: 1; overflow-y: auto;
  padding: var(--spacing-md);
  display: flex; flex-direction: column; gap: 2px;
}

.drawer-item {
  display: flex; align-items: center; gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: var(--transition-fast);
  &:hover { background: var(--color-bg-secondary); color: var(--color-primary); }
}

.drawer-divider { height: 1px; background: var(--color-divider); margin: var(--spacing-sm) 0; }
.drawer-section-title {
  font-size: var(--font-size-xs); font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary); padding: var(--spacing-xs) var(--spacing-md);
  text-transform: uppercase; letter-spacing: 0.5px;
}

.drawer-tags {
  display: flex; flex-wrap: wrap; gap: var(--spacing-xs);
  padding: 0 var(--spacing-md);
}
.drawer-tag {
  font-size: var(--font-size-xs); color: var(--color-text-secondary);
  padding: 2px var(--spacing-sm); border-radius: var(--radius-sm);
  background: var(--color-bg-secondary);
  &:hover { color: var(--color-primary); background: var(--color-primary-50); }
}
</style>
