<template>
  <div class="admin-users-page">
    <el-card shadow="never">
      <div class="page-header">
        <h3>用户管理</h3>
      </div>
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱/昵称" clearable style="width: 300px; margin-bottom: 16px" @input="loadUsers" />
      <el-table :data="users" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" style="margin-right: 4px">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="toggleStatus(row)" :type="row.status === 1 ? 'danger' : 'success'">
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
            <el-button size="small" @click="showRoleDialog(row)">分配角色</el-button>
            <el-popconfirm title="确定重置该用户密码？" @confirm="resetPassword(row)">
              <template #reference>
                <el-button size="small">重置密码</el-button>
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
        @current-change="loadUsers"
      />
    </el-card>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox v-for="role in allRoles" :key="role.id" :label="role.id" :value="role.id">
          {{ role.roleName }} ({{ role.roleCode }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { AdminUserVO, RoleVO } from '@/types'
import { formatTime } from '@/utils'

const users = ref<AdminUserVO[]>([])
const allRoles = ref<RoleVO[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const roleDialogVisible = ref(false)
const selectedRoles = ref<number[]>([])
const currentUser = ref<AdminUserVO | null>(null)

onMounted(() => { loadUsers(); loadRoles() })

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminApi.getUserList({ keyword: keyword.value, page: page.value, size: size.value })
    users.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadRoles() {
  try {
    const res = await adminApi.getRoleList()
    allRoles.value = res.data
  } catch { /* ignore */ }
}

async function toggleStatus(user: AdminUserVO) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await adminApi.updateUserStatus(user.id, { status: newStatus })
    ElMessage.success(newStatus === 0 ? '已封禁' : '已解封')
    loadUsers()
  } catch { /* handled */ }
}

function showRoleDialog(user: AdminUserVO) {
  currentUser.value = user
  selectedRoles.value = user.roles.map(r => allRoles.value.find(rr => rr.roleCode === r)?.id).filter(Boolean) as number[]
  roleDialogVisible.value = true
}

async function saveRoles() {
  if (!currentUser.value) return
  try {
    await adminApi.assignUserRoles(currentUser.value.id, { roleIds: selectedRoles.value })
    ElMessage.success('角色已更新')
    roleDialogVisible.value = false
    loadUsers()
  } catch { /* handled */ }
}

async function resetPassword(user: AdminUserVO) {
  try {
    const res = await adminApi.resetUserPassword(user.id)
    ElMessage.success({ message: `密码已重置为: ${res.data.password}`, duration: 10000, showClose: true })
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); }
</style>
