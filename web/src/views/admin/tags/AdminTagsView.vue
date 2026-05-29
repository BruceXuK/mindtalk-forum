<template>
  <div class="admin-tags">
    <div class="page-header">
      <h2 class="page-title">标签管理</h2>
      <div class="page-header__actions">
        <el-input
          v-model="searchQuery"
          placeholder="搜索标签名..."
          clearable
          size="small"
          style="width: 200px"
          :prefix-icon="Search"
        />
        <el-button
          v-if="selectedRows.length >= 2"
          type="warning"
          size="small"
          @click="openMergeDialog"
        >
          合并选中 ({{ selectedRows.length }})
        </el-button>
        <el-button type="primary" size="small" @click="openDialog()">新增标签</el-button>
      </div>
    </div>

    <el-table
      :data="filteredTags"
      v-loading="loading"
      stripe
      row-key="id"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
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

    <!-- Create/Edit Dialog -->
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

    <!-- Merge Dialog -->
    <el-dialog v-model="mergeVisible" title="合并标签" width="500px">
      <p class="merge-hint">
        将 {{ selectedRows.length }} 个标签合并为一个，请选择保留哪个标签名称。
        其余标签将被删除，帖子关联迁移到保留标签。
      </p>
      <el-radio-group v-model="mergeTargetId" class="merge-list">
        <el-radio
          v-for="tag in selectedRows"
          :key="tag.id"
          :value="tag.id"
          class="merge-option"
        >
          <span class="merge-option__name">{{ tag.name }}</span>
          <span class="merge-option__count">{{ tag.postCount }} 篇帖子</span>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="mergeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMerge" :loading="merging">确认合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api/modules/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TagVO } from '@/types'

const tags = ref<TagVO[]>([])
const loading = ref(false)
const searchQuery = ref('')

const filteredTags = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return tags.value
  return tags.value.filter(t => t.name.toLowerCase().includes(q))
})
const saving = ref(false)
const merging = ref(false)
const dialogVisible = ref(false)
const mergeVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', description: '' })
const selectedRows = ref<TagVO[]>([])
const mergeTargetId = ref<number | null>(null)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getTagList()
    tags.value = res.data || []
  } finally { loading.value = false }
}

function onSelectionChange(rows: TagVO[]) {
  selectedRows.value = rows
}

// ── Create / Edit ──
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

// ── Toggle status ──
async function toggleStatus(row: TagVO) {
  try {
    await adminApi.toggleTagStatus(row.id)
    loadData()
  } catch { /* handled */ }
}

// ── Delete ──
async function handleDelete(row: TagVO) {
  try {
    const msg = row.postCount
      ? `确定删除标签「${row.name}」吗？将同时解除 ${row.postCount} 篇帖子的关联。`
      : `确定删除标签「${row.name}」吗？`
    await ElMessageBox.confirm(msg, '删除确认', { type: 'warning' })
    await adminApi.deleteTag(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled or handled */ }
}

// ── Merge ──
function openMergeDialog() {
  mergeTargetId.value = selectedRows.value[0]?.id || null
  mergeVisible.value = true
}

async function handleMerge() {
  if (!mergeTargetId.value || selectedRows.value.length < 2) return
  merging.value = true
  try {
    const sourceIds = selectedRows.value
      .filter(t => t.id !== mergeTargetId.value)
      .map(t => t.id)
    await adminApi.mergeTags({ sourceIds, targetId: mergeTargetId.value })
    ElMessage.success('合并成功')
    mergeVisible.value = false
    loadData()
  } catch { /* handled */ } finally {
    merging.value = false
  }
}
</script>

<style lang="scss" scoped>
.admin-tags { width: 100%; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}
.page-header__actions {
  display: flex; align-items: center; gap: var(--spacing-sm);
}
.page-title {
  font-size: var(--font-size-xl); font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.merge-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-lg);
  line-height: var(--line-height-relaxed);
}

.merge-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.merge-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);

  &__name {
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-medium);
  }

  &__count {
    font-size: var(--font-size-sm);
    color: var(--color-text-tertiary);
  }
}
</style>
