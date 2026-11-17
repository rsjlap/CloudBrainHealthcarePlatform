<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsCoreOption } from 'echarts'
import { adminDashboardApi, type AdminDashboard } from '@/api/admin'
import EChart from '@/components/EChart.vue'

const loading = ref(false)
const stats = ref<AdminDashboard | null>(null)

const cards = [
  { key: 'todayRegistrations', label: '今日挂号', color: '#409eff' },
  { key: 'inProgressCount', label: '就诊中', color: '#e6a23c' },
  { key: 'completedCount', label: '已完成', color: '#67c23a' },
  { key: 'patientCount', label: '注册患者', color: '#909399' },
  { key: 'doctorCount', label: '在职医生', color: '#626aef' },
] as const

const trendOption = computed<EChartsCoreOption>(() => {
  const trend = stats.value?.registrationTrend ?? []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 32, bottom: 32 },
    xAxis: {
      type: 'category',
      data: trend.map((item) => item.label.slice(5)),
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      name: '挂号数',
    },
    series: [
      {
        name: '挂号量',
        type: 'line',
        smooth: true,
        data: trend.map((item) => item.count),
        areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
        lineStyle: { color: '#409eff', width: 3 },
        itemStyle: { color: '#409eff' },
      },
    ],
  }
})

const deptPieOption = computed<EChartsCoreOption>(() => {
  const dist = stats.value?.departmentDistribution ?? []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        name: '科室挂号',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        data: dist.length
          ? dist.map((item) => ({ name: item.label, value: item.count }))
          : [{ name: '暂无数据', value: 0 }],
        label: { formatter: '{b}\n{d}%' },
      },
    ],
  }
})

async function loadStats() {
  loading.value = true
  try {
    stats.value = await adminDashboardApi()
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in cards" :key="card.key" :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">
            {{ stats?.[card.key] ?? '-' }}
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>业务概况</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="累计挂号">{{ stats?.totalRegistrations ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="今日挂号">{{ stats?.todayRegistrations ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="14" :xs="24">
        <el-card shadow="never">
          <template #header>近 7 天挂号趋势</template>
          <EChart v-if="stats" :option="trendOption" />
        </el-card>
      </el-col>
      <el-col :span="10" :xs="24">
        <el-card shadow="never">
          <template #header>各科室挂号占比</template>
          <EChart v-if="stats" :option="deptPieOption" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page {
  padding: 8px 0;
}

.stat-row {
  margin-bottom: 16px;
}

.chart-row {
  margin-top: 16px;
}

.stat-card {
  margin-bottom: 16px;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}
</style>

