<template>
  <div class="admin-tags">
    <div class="page-header">
      <h2 class="page-title">标签管理</h2>
      <el-button type="primary" size="small" @click="openDialog()">新增标签</el-button>
    </div>

    <el-table :data="tags" v-loading="loading" stripe row-key="id">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="postCount" label="帖子数" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="toggleStatus(row)"
            active-text="启用"
            inactive-text="禁用"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '新增标签'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" placeholder="标签名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" maxlength="200" placeholder="标签描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TagVO } from '@/types'

const tags = ref<TagVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', description: '' })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getTagList()
    tags.value = res.data || []
  } finally { loading.value = false }
}

function openDialog(row?: TagVO) {
  if (row) {
    editingId.value = row.id
    form.value = { name: row.name, description: row.description || '' }
  } else {
    editingId.value = null
    form.value = { name: '', description: '' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateTag(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await adminApi.createTag(form.value)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function toggleStatus(row: TagVO) {
  try {
    await adminApi.toggleTagStatus(row.id)
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row: TagVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除标签「${row.name}」吗？${row.postCount ? `该标签关联了 ${row.postCount} 篇帖子，无法删除。` : ''}`,
      '删除确认',
      { type: 'warning' }
    )
    if (row.postCount) return
    await adminApi.deleteTag(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled or handled */ }
}
</script>

<style lang="scss" scoped>
.admin-tags { width: 100%; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}
.page-title {
  font-size: var(--font-size-xl); font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}
</style>
