<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { dispensePrescriptionApi, getPrescriptionApi, pendingPrescriptionsApi } from '@/api/prescription'
import type { Prescription, RiskLevel } from '@/types/api'
import PharmacistPrescriptionPanel from './components/PharmacistPrescriptionPanel.vue'

const queue = ref<Prescription[]>([])
const loading = ref(false)
const dispensing = ref(false)

const detailVisible = ref(false)
const currentPrescription = ref<Prescription | null>(null)
const detailLoading = ref(false)

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function riskLabel(level?: RiskLevel) {
  if (level === 'HIGH') return '高风险'
  if (level === 'MEDIUM') return '中风险'
  if (level === 'LOW') return '低风险'
  return '未评估'
}

function riskTagType(level?: RiskLevel) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'success'
  return 'info'
}

async function loadQueue() {
  loading.value = true
  try {
    queue.value = await pendingPrescriptionsApi()
  } finally {
    loading.value = false
  }
}

async function openDetail(row: Prescription) {
  detailVisible.value = true
  detailLoading.value = true
  currentPrescription.value = row
  try {
    currentPrescription.value = await getPrescriptionApi(row.id)
  } catch {
    ElMessage.error('加载处方详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function onDispense() {
  if (!currentPrescription.value) return
  const rx = currentPrescription.value
  if (!rx.paid) {
    ElMessage.warning('患者尚未缴纳处方费，请等待缴费后再发药')
    return
  }
  if (rx.riskLevel === 'HIGH') {
    await ElMessageBox.confirm(
      `该处方 AI 审核为高风险：${rx.aiReviewResult || '请仔细核对'}\n确认仍要发药吗？`,
      '高风险处方',
      { type: 'warning' },
    )
  } else {
    await ElMessageBox.confirm('确认发药并扣减库存？', '发药确认', { type: 'warning' })
  }
  dispensing.value = true
  try {
    await dispensePrescriptionApi(rx.id)
    ElMessage.success('发药完成')
    detailVisible.value = false
    currentPrescription.value = null
    await loadQueue()
  } finally {
    dispensing.value = false
  }
}

async function quickDispense(row: Prescription) {
  if (!row.paid) {
    ElMessage.warning('患者尚未缴纳处方费，请等待缴费后再发药')
    return
  }
  currentPrescription.value = row
  await onDispense()
}

onMounted(loadQueue)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2 class="page-title">待发药队列</h2>
      <p class="page-desc">患者已缴费的处方，核对 AI 风险标记后一键发药</p>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>待发药处方（{{ queue.length }}）</span>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadQueue">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="queue" stripe border>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="开方医生" width="100" />
        <el-table-column label="开方时间" width="150">
          <template #default="{ row }: { row: Prescription }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="AI 风险" width="100">
          <template #default="{ row }: { row: Prescription }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">
              {{ riskLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缴费" width="80">
          <template #default="{ row }: { row: Prescription }">
            <el-tag :type="row.paid ? 'success' : 'warning'" size="small">
              {{ row.paid ? '已缴' : '待缴' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="药品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }: { row: Prescription }">
            {{ row.items.map((i) => i.drugName).join('、') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: Prescription }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
            <el-button
              type="success"
              link
              :disabled="!row.paid"
              :loading="dispensing && currentPrescription?.id === row.id"
              @click="quickDispense(row)"
            >
              发药
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && queue.length === 0" description="暂无待发药处方（需患者先缴费）" />
    </el-card>

    <el-drawer
      v-model="detailVisible"
      title="处方详情"
      size="580px"
      destroy-on-close
      @closed="currentPrescription = null"
    >
      <div v-loading="detailLoading">
        <PharmacistPrescriptionPanel
          :prescription="currentPrescription"
          :show-dispense="currentPrescription?.status === 'PENDING_DISPENSE' && currentPrescription?.paid === true"
          :dispensing="dispensing"
          @dispense="onDispense"
        />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page {
  padding: 20px 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
