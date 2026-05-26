<template>
  <div class="search-page">
    <div class="search-input-wrap">
      <el-input v-model="keyword" placeholder="搜索帖子..." size="large" clearable @keyup.enter="doSearch" class="search-input">
        <template #prefix><el-icon :size="18"><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" size="large" :loading="loading" @click="doSearch">搜索</el-button>
    </div>

    <!-- Advanced Filters -->
    <div class="search-filters" v-if="searched || showFilters">
      <div class="filter-row">
        <el-select v-model="filterCategoryId" placeholder="全部分类" clearable size="small" style="width: 160px">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="filterDateRange" placeholder="全部时间" clearable size="small" style="width: 140px" @change="onDateRangeChange">
          <el-option label="最近一周" value="week" />
          <el-option label="最近一月" value="month" />
          <el-option label="最近一年" value="year" />
        </el-select>
        <el-button size="small" text @click="showFilters = !showFilters">
          {{ showFilters ? '收起' : '高级筛选' }}
        </el-button>
      </div>
    </div>

    <div class="hot-searches" v-if="hotSearches.length > 0 && !searched">
      <span class="hot-label">热门搜索：</span>
      <button v-for="(kw, i) in hotSearches" :key="i" class="hot-tag" @click="keyword = kw; doSearch()">
        {{ kw }}
      </button>
    </div>

    <div class="search-results" v-loading="loading">
      <p v-if="results.length > 0 && searched" class="result-count">共找到 {{ total }} 条结果</p>
      <PostCard v-for="post in results" :key="post.id" :post="post" />
      <EmptyState v-if="searched && results.length === 0 && !loading" title="未找到相关帖子" description="试试不同的关键词" />

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: var(--spacing-lg)"
        @current-change="doSearch"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api/modules/search'
import { postApi } from '@/api/modules/post'
import type { PostVO, CategoryVO } from '@/types'
import PostCard from '@/components/post/PostCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { Search } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const keyword = ref((route.query.q as string) || '')
const results = ref<PostVO[]>([])
const hotSearches = ref<string[]>([])
const loading = ref(false)
const searched = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const categories = ref<CategoryVO[]>([])
const filterCategoryId = ref<number | null>(null)
const filterDateRange = ref<string | null>(null)
const filterDateFrom = ref('')
const filterDateTo = ref('')
const showFilters = ref(false)

onMounted(() => {
  loadHotSearches()
  loadCategories()
  if (keyword.value) doSearch()
})

async function loadCategories() {
  try { const res = await postApi.getCategories(); categories.value = res.data as CategoryVO[] } catch { /* ignore */ }
}

function onDateRangeChange(val: string | null) {
  const now = new Date()
  if (val === 'week') {
    const d = new Date(now.getTime() - 7 * 86400000)
    filterDateFrom.value = d.toISOString().split('T')[0]
  } else if (val === 'month') {
    const d = new Date(now.getTime() - 30 * 86400000)
    filterDateFrom.value = d.toISOString().split('T')[0]
  } else if (val === 'year') {
    const d = new Date(now.getTime() - 365 * 86400000)
    filterDateFrom.value = d.toISOString().split('T')[0]
  } else {
    filterDateFrom.value = ''
  }
  if (searched.value) doSearch()
}

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true; searched.value = true
  try {
    router.replace({ query: { q: keyword.value } })
    const params: any = { keyword: keyword.value, page: page.value, size: size.value }
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterDateFrom.value) params.dateFrom = filterDateFrom.value
    const res = await searchApi.search(params)
    results.value = res.data.records; total.value = res.data.total
  } finally { loading.value = false }
}

async function loadHotSearches() {
  try { const res = await searchApi.getHotSearches(); hotSearches.value = res.data } catch { /* ignore */ }
}
</script>

<style lang="scss" scoped>
.search-page { width: 100%; }

.search-input-wrap {
  display: flex; gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  .search-input { flex: 1; }
}

.hot-searches {
  display: flex; flex-wrap: wrap; align-items: center; gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}
.hot-label { font-size: var(--font-size-sm); color: var(--color-text-tertiary); }
.hot-tag {
  font-size: var(--font-size-sm); color: var(--color-text-secondary);
  padding: 4px var(--spacing-md); border-radius: var(--radius-full);
  background: var(--color-bg-secondary); border: none; cursor: pointer;
  transition: var(--transition-fast);
  &:hover { color: var(--color-primary); background: var(--color-primary-50); }
}

.result-count { font-size: var(--font-size-sm); color: var(--color-text-tertiary); margin-bottom: var(--spacing-md); }

@media (max-width: 767px) {
  .search-input-wrap { flex-direction: column; }
}
</style>
