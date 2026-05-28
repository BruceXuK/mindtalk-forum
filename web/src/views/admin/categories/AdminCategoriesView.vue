<template>
  <div class="admin-categories">
    <div class="page-header">
      <h2 class="page-title">分类管理</h2>
      <el-button type="primary" size="small" @click="openDialog()">新增分类</el-button>
    </div>

    <el-table :data="categories" v-loading="loading" stripe row-key="id">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="icon" label="图标" width="80" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" @click="moveUp(row)">上移</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="50" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" maxlength="200" placeholder="分类描述" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" maxlength="200" placeholder="emoji 或图标 URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
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
import type { CategoryVO } from '@/types'

const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', description: '', icon: '', sortOrder: 0 })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getCategoryList()
    categories.value = res.data || []
  } finally { loading.value = false }
}

function openDialog(row?: CategoryVO) {
  if (row) {
    editingId.value = row.id
    form.value = { name: row.name, description: row.description || '', icon: row.icon || '', sortOrder: row.sortOrder || 0 }
  } else {
    editingId.value = null
    form.value = { name: '', description: '', icon: '', sortOrder: 0 }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateCategory(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await adminApi.createCategory(form.value)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function toggleStatus(row: CategoryVO) {
  try {
    await adminApi.toggleCategoryStatus(row.id)
    loadData()
  } catch { /* handled */ }
}

async function moveUp(row: CategoryVO) {
  const idx = categories.value.findIndex(c => c.id === row.id)
  if (idx <= 0) return
  const prev = categories.value[idx - 1]
  const items = [
    { id: row.id, sortOrder: prev.sortOrder ?? idx - 1 },
    { id: prev.id, sortOrder: row.sortOrder ?? idx }
  ]
  try {
    await adminApi.batchSortCategories(items)
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row: CategoryVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？${row.postCount ? `该分类下有 ${row.postCount} 篇帖子，无法删除。` : ''}`,
      '删除确认',
      { type: 'warning' }
    )
    if (row.postCount) return
    await adminApi.deleteCategory(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled or handled */ }
}
</script>

<style lang="scss" scoped>
.admin-categories { width: 100%; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}
.page-title {
  font-size: var(--font-size-xl); font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}
</style>
