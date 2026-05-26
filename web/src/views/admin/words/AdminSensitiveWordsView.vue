<template>
  <div class="admin-words">
    <h2 class="page-title">敏感词管理</h2>
    <div class="add-bar">
      <el-input v-model="newWord" placeholder="输入敏感词" size="small" style="width: 200px" @keyup.enter="addWord" />
      <el-input v-model="newReplacement" placeholder="替换文本(默认***)" size="small" style="width: 160px" @keyup.enter="addWord" />
      <el-button type="primary" size="small" @click="addWord">添加</el-button>
    </div>
    <el-table :data="words" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="word" label="敏感词" width="200" />
      <el-table-column prop="replacement" label="替换为" width="150" />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" text @click="deleteWord(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total"
      layout="prev, pager, next" style="margin-top: var(--spacing-lg); justify-content: center" @current-change="loadWords" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { sensitiveWordApi } from '@/api/modules/sensitiveWord'
import { ElMessage } from 'element-plus'
import { formatTime } from '@/utils'

const words = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(50)
const total = ref(0)
const newWord = ref('')
const newReplacement = ref('')

onMounted(loadWords)

async function loadWords() {
  loading.value = true
  try {
    const res = await sensitiveWordApi.getList({ page: page.value, size: pageSize.value })
    words.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function addWord() {
  if (!newWord.value.trim()) return
  try {
    await sensitiveWordApi.add(newWord.value.trim(), newReplacement.value || '***')
    ElMessage.success('已添加')
    newWord.value = ''
    newReplacement.value = ''
    loadWords()
  } catch { /* handled */ }
}

async function deleteWord(id: number) {
  try {
    await sensitiveWordApi.delete(id)
    ElMessage.success('已删除')
    loadWords()
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.admin-words { width: 100%; }
.page-title { font-size: var(--font-size-xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); margin-bottom: var(--spacing-lg); }
.add-bar { display: flex; gap: var(--spacing-sm); margin-bottom: var(--spacing-lg); }
</style>
