<template>
  <div class="admin-roles-page">
    <el-card shadow="never">
      <div class="page-header"><h3>权限管理</h3></div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="权限树" name="tree">
          <el-tree
            :data="permTree"
            :props="{ children: 'children', label: 'permName' }"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
          >
            <template #default="{ data }">
              <span class="perm-node">
                <el-tag :type="data.permType === 1 ? '' : data.permType === 2 ? 'success' : 'warning'" size="small">
                  {{ data.permType === 1 ? '菜单' : data.permType === 2 ? '按钮' : '接口' }}
                </el-tag>
                <span>{{ data.permName }} <code>{{ data.permCode }}</code></span>
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
        <el-tab-pane label="角色权限" name="roles">
          <el-table :data="roles" stripe style="width: 100%">
            <el-table-column prop="roleName" label="角色名称" width="150" />
            <el-table-column prop="roleCode" label="角色编码" width="130" />
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="showPermDialog(row)">管理权限</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="permDialogVisible" :title="`管理权限 - ${currentRole?.roleName}`" width="500px">
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        :props="{ children: 'children', label: 'permName' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedPermIds"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { RoleVO, PermissionTreeVO } from '@/types'

const roles = ref<RoleVO[]>([])
const permTree = ref<PermissionTreeVO[]>([])
const activeTab = ref('tree')
const permDialogVisible = ref(false)
const currentRole = ref<RoleVO | null>(null)
const checkedPermIds = ref<number[]>([])
const permTreeRef = ref<any>(null)

onMounted(loadData)

async function loadData() {
  const [roleRes, treeRes] = await Promise.all([adminApi.getRoleList(), adminApi.getPermissionTree()])
  roles.value = roleRes.data
  permTree.value = treeRes.data
}

async function showPermDialog(role: RoleVO) {
  currentRole.value = role
  try {
    const res = await adminApi.getRolePermissions(role.id)
    checkedPermIds.value = res.data.map((p: { id: number }) => p.id)
  } catch { checkedPermIds.value = [] }
  permDialogVisible.value = true
}

async function savePerms() {
  if (!currentRole.value) return
  const checked = permTreeRef.value?.getCheckedKeys() || []
  const halfChecked = permTreeRef.value?.getHalfCheckedKeys() || []
  const allIds = [...checked, ...halfChecked]
  try {
    await adminApi.updateRolePermissions(currentRole.value.id, { permissionIds: allIds })
    ElMessage.success('权限已更新')
    permDialogVisible.value = false
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); }
.perm-node { display: flex; align-items: center; gap: 8px; font-size: 14px; code { color: var(--color-text-tertiary); font-size: 12px; } }
</style>
