<template>
  <div class="admin-logs-view">
    <div class="page-header">
      <h2>操作日志</h2>
      <p class="subtitle">查看系统操作记录</p>
    </div>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="操作类型">
          <el-select v-model="filterForm.type" placeholder="请选择操作类型" clearable>
            <el-option label="全部" value="" />
            <el-option label="创建" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作者">
          <el-input v-model="filterForm.operator" placeholder="请输入操作者用户名" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card class="logs-table-card">
      <el-table :data="logs" border :loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="action" label="操作类型" width="120">
          <template #default="scope">
            <el-tag :type="getTagType(scope.row.action)">{{ getActionLabel(scope.row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="操作模块" width="120" />
        <el-table-column prop="detail" label="操作内容" />
        <el-table-column prop="adminName" label="操作者" width="120" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { AdminLogVO } from '@/types'
import { adminApi } from '@/api/modules/admin'

const loading = ref(false)
const logs = ref<AdminLogVO[]>([])

const filterForm = reactive({
  type: '',
  operator: '',
  dateRange: [] as Date[]
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getTagType = (action: string) => {
  const types: Record<string, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'primary',
    LOGOUT: 'info'
  }
  return types[action] || 'default'
}

const getActionLabel = (action: string) => {
  const labels: Record<string, string> = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    LOGIN: '登录',
    LOGOUT: '登出'
  }
  return labels[action] || action
}

const handleSearch = () => {
  pagination.page = 1
  fetchLogs()
}

const handleReset = () => {
  filterForm.type = ''
  filterForm.operator = ''
  filterForm.dateRange = []
  pagination.page = 1
  fetchLogs()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  fetchLogs()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  fetchLogs()
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const response = await adminApi.getLogs({
      page: pagination.page,
      size: pagination.size
    })
    if (response.code === 0) {
      logs.value = response.data.records
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('获取日志失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.admin-logs-view {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.filter-bar {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form {
  gap: 16px;
}

.logs-table-card {
  min-height: 400px;
}
</style>
