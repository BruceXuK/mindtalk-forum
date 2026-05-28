<template>
  <div class="admin-announcements">
    <div class="page-header">
      <h2 class="page-title">公告管理</h2>
      <el-button type="primary" size="small" @click="openDialog()">新增公告</el-button>
    </div>

    <el-table :data="announcements" v-loading="loading" stripe row-key="id">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" width="200" show-overflow-tooltip />
      <el-table-column prop="summary" label="摘要" min-width="150" show-overflow-tooltip />
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPinned" label="置顶" width="70">
        <template #default="{ row }">
          <el-icon v-if="row.isPinned" color="#2563EB"><Check /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" width="170">
        <template #default="{ row }">
          {{ row.expireTime || '永久' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="togglePublish(row)"
          >
            {{ row.status === 1 ? '撤回' : '发布' }}
          </el-button>
          <el-button size="small" @click="togglePin(row)">
            {{ row.isPinned ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '新增公告'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" maxlength="500" placeholder="简短摘要（展示在横幅）" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" placeholder="公告正文（支持 Markdown）" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width: 100%">
            <el-option
              v-for="l in levelDict"
              :key="l.itemKey"
              :label="`${l.itemKey} - ${l.itemValue}`"
              :value="l.itemKey"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isPinned" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="留空表示永久有效"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
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
import { dictApi, type DictItem } from '@/api/modules/dict'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import type { AnnouncementVO } from '@/types'

const announcements = ref<AnnouncementVO[]>([])
const levelDict = ref<DictItem[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  title: '', summary: '', content: '', level: 'INFO',
  isPinned: false, expireTime: '', sortOrder: 0
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [dictRes, annRes] = await Promise.all([
      dictApi.getItems('ANNOUNCE_LEVEL'),
      adminApi.getAnnouncementList()
    ])
    levelDict.value = dictRes.data || []
    announcements.value = annRes.data || []
  } finally { loading.value = false }
}

function openDialog(row?: AnnouncementVO) {
  if (row) {
    editingId.value = row.id
    form.value = {
      title: row.title, summary: row.summary || '', content: row.content || '',
      level: row.level, isPinned: row.isPinned,
      expireTime: row.expireTime || '', sortOrder: row.sortOrder || 0
    }
  } else {
    editingId.value = null
    form.value = { title: '', summary: '', content: '', level: 'INFO', isPinned: false, expireTime: '', sortOrder: 0 }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      expireTime: form.value.expireTime || undefined
    }
    if (editingId.value) {
      await adminApi.updateAnnouncement(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await adminApi.createAnnouncement(payload as any)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function togglePublish(row: AnnouncementVO) {
  try {
    await adminApi.toggleAnnouncementPublish(row.id)
    ElMessage.success(row.status === 1 ? '已撤回' : '已发布')
    loadData()
  } catch { /* handled */ }
}

async function togglePin(row: AnnouncementVO) {
  try {
    await adminApi.toggleAnnouncementPin(row.id)
    ElMessage.success(row.isPinned ? '已取消置顶' : '已置顶')
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(row: AnnouncementVO) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
    await adminApi.deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

function levelTag(level: string) {
  const item = levelDict.value.find(d => d.itemKey === level)
  return item?.extra || ''
}
function levelLabel(level: string) {
  const item = levelDict.value.find(d => d.itemKey === level)
  return item?.itemValue || level
}
function statusTag(status: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] || 'info'
}
function statusLabel(status: number) {
  const map: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已撤回' }
  return map[status] || '未知'
}
</script>

<style lang="scss" scoped>
.admin-announcements { width: 100%; }
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}
.page-title {
  font-size: var(--font-size-xl); font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}
</style>
