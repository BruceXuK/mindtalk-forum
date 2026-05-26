<template>
  <div class="admin-posts-page">
    <el-card shadow="never">
      <div class="page-header">
        <h3>帖子审核</h3>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px" @change="loadPosts">
          <el-option label="全部" :value="undefined" />
          <el-option label="正常" :value="1" />
          <el-option label="审核中" :value="2" />
        </el-select>
      </div>
      <el-table :data="posts" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="作者" width="120">
          <template #default="{ row }">{{ row.author?.nickname }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '正常' : '审核中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="100">
          <template #default="{ row }">
            <span v-if="row.isPinned" class="expiry-info">
              置顶
              <span v-if="row.pinnedUntil" class="expiry-date">至{{ formatDate(row.pinnedUntil) }}</span>
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="精华" width="100">
          <template #default="{ row }">
            <span v-if="row.isFeatured" class="expiry-info">
              精华
              <span v-if="row.featuredUntil" class="expiry-date">至{{ formatDate(row.featuredUntil) }}</span>
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="70" prop="viewCount" />
        <el-table-column label="评论" width="70" prop="commentCount" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" size="small" type="success" @click="auditPost(row, 1)">通过</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="auditPost(row, 2)">驳回</el-button>
            <el-button size="small" @click="openPinDialog(row)">
              {{ row.isPinned ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button size="small" @click="openFeatureDialog(row)">
              {{ row.isFeatured ? '取消精华' : '精华' }}
            </el-button>
            <el-popconfirm title="确定删除？" @confirm="deletePost(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px"
        @current-change="loadPosts"
      />
    </el-card>

    <!-- 置顶时长弹窗 -->
    <el-dialog v-model="pinDialogVisible" title="设置置顶时长" width="400px">
      <div class="expiry-options">
        <el-radio-group v-model="pinDuration" class="expiry-radio-group">
          <el-radio :label="1">1天</el-radio>
          <el-radio :label="3">3天</el-radio>
          <el-radio :label="7">7天</el-radio>
          <el-radio :label="30">30天</el-radio>
          <el-radio :label="0">永久</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="pinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPin">确认置顶</el-button>
      </template>
    </el-dialog>

    <!-- 加精时长弹窗 -->
    <el-dialog v-model="featureDialogVisible" title="设置加精时长" width="400px">
      <div class="expiry-options">
        <el-radio-group v-model="featureDuration" class="expiry-radio-group">
          <el-radio :label="1">1天</el-radio>
          <el-radio :label="3">3天</el-radio>
          <el-radio :label="7">7天</el-radio>
          <el-radio :label="30">30天</el-radio>
          <el-radio :label="0">永久</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="featureDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmFeature">确认加精</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { PostVO } from '@/types'
import { formatTime } from '@/utils'

const posts = ref<PostVO[]>([])
const loading = ref(false)
const filterStatus = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const pinDialogVisible = ref(false)
const featureDialogVisible = ref(false)
const pinDuration = ref(0)
const featureDuration = ref(0)
const currentPost = ref<PostVO | null>(null)

onMounted(loadPosts)

async function loadPosts() {
  loading.value = true
  try {
    const res = await adminApi.getPostList({ status: filterStatus.value, page: page.value, size: size.value })
    posts.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function auditPost(post: PostVO, status: number) {
  try {
    await adminApi.auditPost(post.id, { status })
    ElMessage.success(status === 1 ? '审核通过' : '已驳回')
    loadPosts()
  } catch { /* handled */ }
}

async function deletePost(id: number) {
  try {
    await adminApi.deletePost(id)
    ElMessage.success('已删除')
    loadPosts()
  } catch { /* handled */ }
}

function openPinDialog(post: PostVO) {
  if (post.isPinned) {
    confirmUnpin(post)
    return
  }
  currentPost.value = post
  pinDuration.value = 0
  pinDialogVisible.value = true
}

async function confirmUnpin(post: PostVO) {
  try {
    await adminApi.pinPost(post.id, false)
    ElMessage.success('已取消置顶')
    loadPosts()
  } catch { /* handled */ }
}

async function confirmPin() {
  if (!currentPost.value) return
  const untilDays = pinDuration.value === 0 ? undefined : pinDuration.value
  try {
    await adminApi.pinPost(currentPost.value.id, true, untilDays)
    ElMessage.success(pinDuration.value === 0 ? '已永久置顶' : `已置顶${pinDuration.value}天`)
    pinDialogVisible.value = false
    loadPosts()
  } catch { /* handled */ }
}

function openFeatureDialog(post: PostVO) {
  if (post.isFeatured) {
    confirmUnfeature(post)
    return
  }
  currentPost.value = post
  featureDuration.value = 0
  featureDialogVisible.value = true
}

async function confirmUnfeature(post: PostVO) {
  try {
    await adminApi.featurePost(post.id, false)
    ElMessage.success('已取消精华')
    loadPosts()
  } catch { /* handled */ }
}

async function confirmFeature() {
  if (!currentPost.value) return
  const untilDays = featureDuration.value === 0 ? undefined : featureDuration.value
  try {
    await adminApi.featurePost(currentPost.value.id, true, untilDays)
    ElMessage.success(featureDuration.value === 0 ? '已永久加精' : `已加精${featureDuration.value}天`)
    featureDialogVisible.value = false
    loadPosts()
  } catch { /* handled */ }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); }
.expiry-info { font-size: var(--font-size-xs); }
.expiry-date { color: var(--color-text-tertiary); display: block; font-size: 11px; }
.text-muted { color: var(--color-text-tertiary); }
.expiry-options { padding: var(--spacing-md) 0; }
.expiry-radio-group { display: flex; flex-direction: column; gap: var(--spacing-md); }
</style>
