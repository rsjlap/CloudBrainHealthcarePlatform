<script setup lang="ts">
import type { ExamOrder, ExamReport } from '@/api/exam'

defineProps<{
  order: ExamOrder | null
  report: ExamReport | null
}>()

const visible = defineModel<boolean>({ default: false })

function formatDateTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="order ? `${order.examType} — 检查报告` : '检查报告'"
    width="680px"
    destroy-on-close
  >
    <template v-if="order && report">
      <div class="meta-row">
        <el-tag size="small">{{ order.orderType === 'LAB' ? '检验' : '检查' }}</el-tag>
        <span v-if="order.bodyPart" class="meta-item">{{ order.bodyPart }}</span>
        <span v-if="report.inspectorName" class="meta-item">检查医师：{{ report.inspectorName }}</span>
        <span v-if="report.confirmedAt" class="meta-item">发布时间：{{ formatDateTime(report.confirmedAt) }}</span>
      </div>
      <section class="report-block">
        <div class="block-title">检查所见 / 数据</div>
        <div class="block-content">{{ report.reportText || '暂无' }}</div>
      </section>
      <section class="report-block">
        <div class="block-title">结论</div>
        <div class="block-content conclusion">{{ report.conclusion || '暂无' }}</div>
      </section>
    </template>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #606266;
  font-size: 13px;
}

.meta-item {
  color: #909399;
}

.report-block + .report-block {
  margin-top: 16px;
}

.block-title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.block-content {
  padding: 12px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  font-size: 14px;
}

.conclusion {
  background: #f0f9eb;
  color: #303133;
}
</style>
