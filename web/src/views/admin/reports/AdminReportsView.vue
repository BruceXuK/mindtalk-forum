<template>
  <div class="admin-reports-page">
    <el-card shadow="never">
      <div class="page-header">
        <h3>举报处理</h3>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 130px" @change="loadReports">
          <el-option label="全部" :value="undefined" />
          <el-option label="待处理" :value="1" />
          <el-option label="已处理" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>
      </div>
      <el-table :data="reports" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="举报人" width="120">
          <template #default="{ row }">{{ row.reporter?.nickname }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80" prop="targetType" />
        <el-table-column label="目标ID" width="80" prop="targetId" />
        <el-table-column prop="reason" label="原因" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : row.status === 2 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '待处理' : row.status === 2 ? '已处理' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button size="small" type="success" @click="handleDialog(row)">处理</el-button>
            </template>
            <span v-else class="handled-text">已处理</span>
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
        @current-change="loadReports"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="处理举报" width="440px">
      <p><strong>举报原因：</strong>{{ currentReport?.reason }}</p>
      <p><strong>描述：</strong>{{ currentReport?.description || '无' }}</p>
      <el-form label-position="top" style="margin-top: 16px">
        <el-form-item label="处理结果">
          <el-input v-model="handleResult" type="textarea" :rows="3" placeholder="请输入处理结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitHandle(3)">驳回</el-button>
        <el-button type="primary" @click="submitHandle(2)">标记已处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { ReportVO } from '@/types'
import { formatTime } from '@/utils'

const reports = ref<ReportVO[]>([])
const loading = ref(false)
const filterStatus = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const currentReport = ref<ReportVO | null>(null)
const handleResult = ref('')

onMounted(loadReports)

async function loadReports() {
  loading.value = true
  try {
    const res = await adminApi.getReportList({ status: filterStatus.value, page: page.value, size: size.value })
    reports.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleDialog(report: ReportVO) {
  currentReport.value = report
  handleResult.value = ''
  dialogVisible.value = true
}

async function submitHandle(status: number) {
  if (!currentReport.value) return
  try {
    await adminApi.handleReport(currentReport.value.id, { status, handleResult: handleResult.value })
    ElMessage.success('已处理')
    dialogVisible.value = false
    loadReports()
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); }
.handled-text { color: var(--color-text-tertiary); font-size: var(--font-size-sm); }
</style>
