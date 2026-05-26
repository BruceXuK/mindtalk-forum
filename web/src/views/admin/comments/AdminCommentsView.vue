<template>
  <div class="admin-comments-page">
    <el-card shadow="never">
      <div class="page-header"><h3>评论审核</h3></div>
      <el-table :data="comments" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="content" label="内容" min-width="250" />
        <el-table-column label="作者" width="120">
          <template #default="{ row }">{{ row.user?.nickname }}</template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除？" @confirm="deleteComment(row.id)">
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
        @current-change="loadComments"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { CommentVO } from '@/types'
import { formatTime } from '@/utils'

const comments = ref<CommentVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

onMounted(loadComments)

async function loadComments() {
  loading.value = true
  try {
    const res = await adminApi.getCommentList({ page: page.value, size: size.value })
    comments.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function deleteComment(id: number) {
  try {
    await adminApi.deleteComment(id)
    ElMessage.success('已删除')
    loadComments()
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); }
</style>
