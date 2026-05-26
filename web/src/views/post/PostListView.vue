<template>
  <div class="post-list-page">
    <div class="list-header">
      <h2>全部帖子</h2>
      <el-button v-if="userStore.isLoggedIn" type="primary" size="small" round @click="$router.push('/posts/create')">
        <el-icon :size="16"><EditPen /></el-icon> 发布帖子
      </el-button>
    </div>

    <div class="post-feed" :class="{ 'is-loading': loading }">
      <TransitionGroup name="feed" tag="div">
        <PostCard v-for="post in posts" :key="post.id" :post="post" />
      </TransitionGroup>
    </div>

    <SkeletonCard v-if="loading" :count="5" />
    <EmptyState v-if="posts.length === 0 && !loading" title="暂无帖子" description="还没有人发布帖子" />

    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="prev, pager, next"
      @current-change="loadPosts"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { postApi } from '@/api/modules/post'
import { useUserStore } from '@/stores/modules/user'
import type { PostVO } from '@/types'
import PostCard from '@/components/post/PostCard.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { EditPen } from '@element-plus/icons-vue'

const route = useRoute()
const userStore = useUserStore()
const posts = ref<PostVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const categoryId = computed(() => {
  const v = route.query.categoryId
  return v ? Number(v) : undefined
})
const tagId = computed(() => {
  const v = route.query.tagId
  return v ? Number(v) : undefined
})

onMounted(loadPosts)

watch([categoryId, tagId], () => {
  page.value = 1
  loadPosts()
})

async function loadPosts() {
  loading.value = posts.value.length === 0
  try {
    const res = await postApi.getList({
      page: page.value, size: size.value, orderBy: 'create_time',
      categoryId: categoryId.value,
      tagId: tagId.value
    })
    posts.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.post-list-page { width: 100%; }

.list-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--spacing-lg);
  h2 { font-size: var(--font-size-xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
}

.post-feed {
  min-height: 200px;

  &.is-loading {
    opacity: 0.5;
    pointer-events: none;
    transition: opacity 0.15s ease;
  }
}

.feed-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.feed-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; position: absolute; }
.feed-enter-from { opacity: 0; transform: translateY(8px); }
.feed-leave-to { opacity: 0; }
.feed-move { transition: transform 0.25s ease; }
</style>
