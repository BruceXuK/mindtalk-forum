<template>
  <div class="admin-stats-page">
    <!-- 概览卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col v-for="card in overviewCards" :key="card.label" :span="6">
        <el-card shadow="never" :body-style="{ padding: '20px' }">
          <div class="stat-card">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势折线图 -->
    <el-card shadow="never" header="最近 7 天趋势" style="margin-bottom: 20px">
      <div ref="trendChartRef" style="height: 300px"></div>
    </el-card>

    <el-row :gutter="16">
      <!-- 分类饼图 -->
      <el-col :span="12">
        <el-card shadow="never" header="内容分类分布">
          <div ref="pieChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <!-- 时段柱状图 -->
      <el-col :span="12">
        <el-card shadow="never" header="热门发帖时段（近30天）">
          <div ref="barChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { adminApi } from '@/api/modules/admin'
import type { StatsOverviewVO, StatsTrendVO } from '@/types'
import * as echarts from 'echarts'

const overview = ref<StatsOverviewVO | null>(null)
const trends = ref<StatsTrendVO[]>([])
const loading = ref(false)
const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()

const overviewCards = computed(() => {
  if (!overview.value) return []
  return [
    { label: '用户总数', value: overview.value.totalUsers, color: '#409eff' },
    { label: '帖子总数', value: overview.value.totalPosts, color: '#67c23a' },
    { label: '评论总数', value: overview.value.totalComments, color: '#e6a23c' },
    { label: '待处理举报', value: overview.value.pendingReports, color: '#f56c6c' },
    { label: '今日新增用户', value: overview.value.todayNewUsers, color: '#409eff' },
    { label: '今日新增帖子', value: overview.value.todayNewPosts, color: '#67c23a' },
    { label: '今日新增评论', value: overview.value.todayNewComments, color: '#e6a23c' },
    { label: '举报总数', value: overview.value.totalReports, color: '#909399' }
  ]
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [ovRes, trRes] = await Promise.all([
      adminApi.getStatsOverview(),
      adminApi.getStatsTrends(7)
    ])
    overview.value = ovRes.data
    trends.value = trRes.data
    await nextTick()
    renderTrendChart()
  } finally { loading.value = false }
  loadChartData()
}

async function loadChartData() {
  try {
    const [catRes, hourRes] = await Promise.all([
      adminApi.getCategoryDistribution(),
      adminApi.getHourlyActivity()
    ])
    await nextTick()
    renderPieChart(catRes.data)
    renderBarChart(hourRes.data)
  } catch { /* ignore */ }
}

function renderTrendChart() {
  if (!trendChartRef.value || trends.value.length === 0) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '新增帖子', '新增评论'], bottom: 0 },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: trends.value.map(t => t.date) },
    yAxis: { type: 'value' },
    series: [
      { name: '新增用户', type: 'line', data: trends.value.map(t => t.newUsers), smooth: true, color: '#409eff' },
      { name: '新增帖子', type: 'line', data: trends.value.map(t => t.newPosts), smooth: true, color: '#67c23a' },
      { name: '新增评论', type: 'line', data: trends.value.map(t => t.newComments), smooth: true, color: '#e6a23c' }
    ]
  })
}

function renderPieChart(data: any[]) {
  if (!pieChartRef.value || !data || data.length === 0) return
  const chart = echarts.init(pieChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: data.map((d: any) => ({ name: d.name, value: d.value })),
      label: { formatter: '{b}\n{d}%' }
    }]
  })
}

function renderBarChart(data: any[]) {
  if (!barChartRef.value || !data || data.length === 0) return
  const chart = echarts.init(barChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: data.map((d: any) => d.hour + ':00') },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar', data: data.map((d: any) => d.count),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
    }]
  })
}
</script>

<style lang="scss" scoped>
.stat-card { text-align: center; }
.stat-label { font-size: var(--font-size-sm); color: var(--color-text-tertiary); margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; }

@media (max-width: 1200px) {
  .el-col { flex: 0 0 50%; max-width: 50%; margin-bottom: 12px; }
}
</style>
