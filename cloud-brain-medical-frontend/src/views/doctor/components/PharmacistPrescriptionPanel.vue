<script setup lang="ts">
import type { Prescription, RiskLevel } from '@/types/api'

defineProps<{
  prescription: Prescription | null
  showDispense?: boolean
  dispensing?: boolean
}>()

const emit = defineEmits<{
  dispense: []
}>()

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
</script>

<template>
  <template v-if="prescription">
    <el-descriptions :column="2" border size="small" class="detail-desc">
      <el-descriptions-item label="患者">{{ prescription.patientName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="开方医生">{{ prescription.doctorName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="处方编号">#{{ prescription.id }}</el-descriptions-item>
      <el-descriptions-item label="开方时间">{{ formatDateTime(prescription.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="AI 风险">
        <el-tag :type="riskTagType(prescription.riskLevel)" size="small">
          {{ riskLabel(prescription.riskLevel) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="缴费状态">
        <el-tag :type="prescription.paid ? 'success' : 'warning'" size="small">
          {{ prescription.paid ? '已缴费' : '待缴费' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag
          :type="prescription.status === 'DISPENSED' ? 'success' : 'warning'"
          size="small"
        >
          {{ prescription.status === 'DISPENSED' ? '已发药' : '待发药' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="prescription.dispensedAt" label="发药时间">
        {{ formatDateTime(prescription.dispensedAt) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="prescription.dispensedByName" label="发药药师">
        {{ prescription.dispensedByName }}
      </el-descriptions-item>
    </el-descriptions>

    <section class="section">
      <div class="section-title">处方明细 Rp</div>
      <el-table :data="prescription.items" border size="small">
        <el-table-column prop="drugName" label="药品" min-width="140" />
        <el-table-column prop="drugCode" label="编码" width="90" />
        <el-table-column label="用量" width="90">
          <template #default="{ row }">
            {{ row.dosage }}{{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="frequency" label="频次" width="90" />
        <el-table-column prop="durationDays" label="天数" width="70" />
        <el-table-column prop="totalAmount" label="总量" width="80" />
      </el-table>
    </section>

    <section v-if="prescription.aiReviewResult" class="section">
      <div class="section-title">AI 审核结果</div>
      <div class="ai-review">{{ prescription.aiReviewResult }}</div>
    </section>
    <section v-else class="section">
      <div class="section-title">AI 审核结果</div>
      <p class="muted">暂无 AI 审核记录</p>
    </section>

    <div v-if="showDispense" class="footer-actions">
      <el-button
        type="primary"
        size="large"
        :loading="dispensing"
        @click="emit('dispense')"
      >
        确认发药
      </el-button>
    </div>
  </template>
</template>

<style scoped>
.detail-desc {
  margin-bottom: 20px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.ai-review {
  padding: 12px 14px;
  background: #fdf6ec;
  border-radius: 6px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: #606266;
  font-size: 14px;
}

.muted {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.footer-actions {
  margin-top: 24px;
  text-align: center;
}
</style>
