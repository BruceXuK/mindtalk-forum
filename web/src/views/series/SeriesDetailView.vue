<template>
  <div class="series-detail" v-loading="loading">
    <template v-if="series">
      <!-- Header -->
      <div class="series-header card-base">
        <div class="series-header__info">
          <h1 class="series-title">{{ series.title }}</h1>
          <p class="series-desc" v-if="series.description">{{ series.description }}</p>
          <div class="series-meta">
            <router-link :to="`/users/${series.author?.id}`" class="series-author">
              <el-avatar :size="24" :src="series.author?.avatarUrl">
                {{ series.author?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span>{{ series.author?.nickname }}</span>
            </router-link>
            <span class="meta-sep">·</span>
            <span>{{ series.postCount }} 篇帖子</span>
            <span class="meta-sep">·</span>
            <span>{{ formatDate(series.createTime) }} 创建</span>
          </div>
        </div>
        <div class="series-header__actions" v-if="isOwner">
          <el-button size="small" type="primary" @click="router.push(`/posts/create?seriesId=${seriesId}`)">写帖子</el-button>
          <el-button size="small" @click="showEditDialog = true">编辑</el-button>
          <el-popconfirm title="确定删除该系列？" @confirm="handleDelete">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <!-- Post List -->
      <div class="series-posts">
        <PostCard v-for="p in series.posts" :key="p.id" :post="p" />
        <EmptyState v-if="series.posts.length === 0" title="暂无帖子" :description="isOwner ? '在系列中发布你的第一篇帖子' : '该系列还没有收录帖子'">
          <template v-if="isOwner" #action>
            <el-button type="primary" @click="router.push(`/posts/create?seriesId=${seriesId}`)">开始写作</el-button>
          </template>
        </EmptyState>
      </div>

      <!-- Edit Dialog -->
      <el-dialog v-model="showEditDialog" title="编辑系列" width="480px">
        <el-form :model="editForm" label-position="top">
          <el-form-item label="系列标题" required>
            <el-input v-model="editForm.title" maxlength="200" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="editForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="封面图 URL">
            <el-input v-model="editForm.coverUrl" placeholder="可选" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="editForm.status">
              <el-radio :value="1">公开</el-radio>
              <el-radio :value="0">私密</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
        </template>
      </el-dialog>
    </template>
    <EmptyState v-else-if="!loading" title="系列不存在" description="该系列可能已被删除或不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { seriesApi } from '@/api/modules/series'
import { useUserStore } from '@/stores/modules/user'
import type { SeriesDetailVO } from '@/types'
import PostCard from '@/components/post/PostCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const seriesId = computed(() => Number(route.params.id))
const series = ref<SeriesDetailVO | null>(null)
const loading = ref(false)
const saving = ref(false)
const showEditDialog = ref(false)
const editForm = ref({ title: '', description: '', coverUrl: '', status: 1 })

const isOwner = computed(() =>
  userStore.isLoggedIn && series.value?.author?.id === userStore.userInfo?.id
)

onMounted(loadSeries)

async function loadSeries() {
  loading.value = true
  try {
    const res = await seriesApi.getDetail(seriesId.value)
    series.value = res.data
  } catch { /* handled */ }
  finally { loading.value = false }
}

function handleDelete() {
  seriesApi.delete(seriesId.value).then(() => {
    ElMessage.success('已删除')
    router.push('/')
  }).catch(() => {})
}

function saveEdit() {
  if (!editForm.value.title.trim()) return
  saving.value = true
  seriesApi.update(seriesId.value, editForm.value).then(res => {
    series.value = res.data
    showEditDialog.value = false
    ElMessage.success('已更新')
  }).catch(() => {}).finally(() => { saving.value = false })
}

// Sync edit form when opening dialog
import { watch } from 'vue'
watch(showEditDialog, (v) => {
  if (v && series.value) {
    editForm.value = {
      title: series.value.title,
      description: series.value.description || '',
      coverUrl: series.value.coverUrl || '',
      status: 1
    }
  }
})
</script>

<style lang="scss" scoped>
.series-detail {
  max-width: 760px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

.series-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-lg);

  &__info { flex: 1; min-width: 0; }
  &__actions { flex-shrink: 0; display: flex; gap: var(--spacing-sm); margin-left: var(--spacing-lg); }
}

.series-title {
  font-size: 28px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-sm);
}

.series-desc {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
  line-height: var(--line-height-normal);
}

.series-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.series-author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);

  &:hover { color: var(--color-primary); }
}

.meta-sep { color: var(--color-border); }

@media (max-width: 767px) {
  .series-header {
    flex-direction: column;
    padding: var(--spacing-lg);

    &__actions { margin-left: 0; margin-top: var(--spacing-md); }
  }

  .series-title { font-size: 22px; }
}
</style>
