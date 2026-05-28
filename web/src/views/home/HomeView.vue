<template>
  <div class="home-page">
    <AnnouncementBar />
    <!-- Hero / Welcome -->
    <section class="hero">
      <h1 class="hero-title">{{ config.home.heroTitle }}</h1>
      <p class="hero-sub">{{ config.home.heroSubtitle }}</p>
      <div class="hero-search" @click="$router.push('/search')">
        <el-icon :size="18"><Search /></el-icon>
        <span>{{ config.search.homePlaceholder }}</span>
        <kbd>{{ config.hotkeys.search }}</kbd>
      </div>
    </section>

    <!-- Feed Tabs -->
    <div class="feed-tabs">
      <button
        v-for="tab in config.home.feedTabs"
        :key="tab.key"
        v-show="tab.key !== 'following' || userStore.isLoggedIn"
        class="feed-tab"
        :class="{ active: feedTab === tab.key }"
        @click="switchFeed(tab.key as any)"
      >{{ tab.label }}</button>
    </div>

    <!-- Ranking Period Selector -->
    <div class="ranking-period" v-if="feedTab === 'ranking'">
      <button
        v-for="period in config.home.rankingPeriods"
        :key="period.key"
        class="period-btn"
        :class="{ active: rankPeriod === period.key }"
        @click="rankPeriod = period.key as any; loadRanking()"
      >{{ period.label }}</button>
    </div>

    <!-- 分类筛选 -->
    <div class="category-bar" v-if="feedTab !== 'ranking'">
      <button
        v-for="cat in [{ id: 0, name: '全部' }, ...categories]"
        :key="cat.id"
        class="cat-pill"
        :class="{ active: currentCategory === cat.id }"
        @click="switchCategory(cat.id)"
      >
        {{ cat.icon && cat.id !== 0 ? cat.icon + ' ' : '' }}{{ cat.name }}
      </button>
    </div>

    <!-- 帖子列表 -->
    <div class="post-feed" :class="{ 'is-loading': loading }">
      <TransitionGroup name="feed" tag="div">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
        />
      </TransitionGroup>
    </div>

    <!-- 加载骨架 -->
    <SkeletonCard v-if="loading" :count="4" />

    <!-- 空状态 -->
    <EmptyState
      v-if="posts.length === 0 && !loading"
      title="暂无帖子"
      description="还没有人发布帖子，成为第一个分享者吧"
      :action-text="userStore.isLoggedIn ? '发布帖子' : ''"
      @action="$router.push('/posts/create')"
    />

    <!-- Infinite scroll sentinel -->
    <div ref="sentinelRef" class="scroll-sentinel" v-if="posts.length < total && !loading"></div>
    <div class="load-more-status" v-if="loadingMore">加载中...</div>
    <div class="load-more-status" v-if="posts.length >= total && total > 0 && !loading">已加载全部</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { postApi } from '@/api/modules/post'
import { useUserStore } from '@/stores/modules/user'
import { useAppConfig } from '@/composables/useAppConfig'
import type { PostVO, CategoryVO } from '@/types'
import PostCard from '@/components/post/PostCard.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import AnnouncementBar from '@/components/announcement/AnnouncementBar.vue'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const config = useAppConfig()
const posts = ref<PostVO[]>([])
const categories = ref<CategoryVO[]>([])
const currentCategory = ref(0)
const feedTab = ref<string>(config.home.feedTabs[0].key)
const rankPeriod = ref<string>(config.home.rankingPeriods[0].key)
const page = ref(1)
const size = ref(config.pagination.homePageSize)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const sentinelRef = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

onMounted(() => {
  loadPosts()
  loadCategories()
  setupObserver()
})

onUnmounted(() => {
  observer?.disconnect()
})

function setupObserver() {
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && !loading.value && !loadingMore.value && posts.value.length < total.value) {
      loadingMore.value = true
      page.value++
      loadPosts()
    }
  }, { rootMargin: '200px' })
  nextTick(() => {
    if (sentinelRef.value) observer?.observe(sentinelRef.value)
  })
}

async function loadPosts() {
  loading.value = posts.value.length === 0
  try {
    const params: any = { page: page.value, size: size.value, orderBy: 'createTime' }
    if (currentCategory.value > 0) params.categoryId = currentCategory.value

    let res
    if (feedTab.value === 'following') {
      res = await postApi.getFollowingFeed(params)
    } else if (feedTab.value === 'ranking') {
      await loadRanking()
      loading.value = false
      return
    } else if (feedTab.value === 'recommended') {
      const recRes = await postApi.getRecommended(config.pagination.recommendedSize)
      posts.value = recRes.data || []
      total.value = posts.value.length
      loading.value = false
      return
    } else {
      res = await postApi.getList(params)
    }
    if (page.value === 1) {
      posts.value = res.data.records
    } else {
      posts.value.push(...res.data.records)
    }
    total.value = res.data.total
  } finally { loading.value = false; loadingMore.value = false }
}

async function loadCategories() {
  try {
    const res = await postApi.getCategories()
    categories.value = (res.data as CategoryVO[]) || []
  } catch { /* ignore */ }
}

function switchCategory(id: number) {
  currentCategory.value = id; page.value = 1; posts.value = []; total.value = 0; loadPosts()
}

watch(posts, () => {
  nextTick(() => {
    if (sentinelRef.value && posts.value.length < total.value) {
      observer?.disconnect()
      observer?.observe(sentinelRef.value)
    }
  })
})

function switchFeed(tab: string) {
  feedTab.value = tab; page.value = 1; posts.value = []; total.value = 0; loadPosts()
}

async function loadRanking() {
  const res = await postApi.getRanking(rankPeriod.value, config.pagination.rankingSize)
  posts.value = res.data || []
  total.value = posts.value.length
}
</script>

<style lang="scss" scoped>
.home-page {
  width: 100%;
}

// ── Hero ──
.hero {
  text-align: center;
  padding: var(--spacing-2xl) 0 var(--spacing-xl);
}

.hero-title {
  font-size: 32px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
  margin-bottom: var(--spacing-xs);
}

.hero-sub {
  font-size: var(--font-size-md);
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
}

.hero-search {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  max-width: 520px;
  margin: 0 auto;
  height: 48px;
  padding: 0 var(--spacing-lg);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);

  &:hover {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
  }

  span {
    flex: 1;
    text-align: left;
  }

  kbd {
    font-size: var(--font-size-xs);
    padding: 2px 8px;
    background: var(--color-bg-secondary);
    border-radius: 4px;
    border: 1px solid var(--color-divider);
    color: var(--color-text-tertiary);
  }
}

// ── Feed Tabs ──
.feed-tabs {
  display: flex;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-divider);
}

.feed-tab {
  padding: 6px 18px;
  border: none;
  background: none;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }

  &.active {
    background: var(--color-text-primary);
    color: #fff;
  }
}

// ── Ranking Period ──
.ranking-period {
  display: flex;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-lg);
}

.period-btn {
  padding: 4px 14px;
  border: none;
  background: none;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }
  &.active { background: var(--color-primary); color: #fff; }
}

// ── Category Bar ──
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);
}

.cat-pill {
  padding: 5px 14px;
  border-radius: var(--radius-full);
  border: none;
  background: none;
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;

  &:hover {
    color: var(--color-text-primary);
    background: var(--color-bg-secondary);
  }

  &.active {
    background: var(--color-text-primary);
    color: #fff;
  }
}

// ── Post Feed Transition ──
.post-feed {
  min-height: 200px;

  &.is-loading {
    opacity: 0.5;
    pointer-events: none;
    transition: opacity 0.15s ease;
  }
}

.feed-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.feed-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
  position: absolute;
}

.feed-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.feed-leave-to {
  opacity: 0;
}

.feed-move {
  transition: transform 0.25s ease;
}

// ── Infinite Scroll ──
.scroll-sentinel { height: 1px; }
.load-more-status { text-align: center; padding: var(--spacing-lg) 0; font-size: var(--font-size-sm); color: var(--color-text-tertiary); }

@media (max-width: 767px) {
  .hero {
    padding: var(--spacing-lg) 0;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-search {
    height: 42px;
    kbd { display: none; }
  }

  .category-bar {
    gap: 6px;
  }

  .cat-pill {
    padding: 4px 12px;
    font-size: var(--font-size-xs);
  }
}
</style>
