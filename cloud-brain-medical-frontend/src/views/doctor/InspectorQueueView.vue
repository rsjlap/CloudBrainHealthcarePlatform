<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  confirmExamReportApi,
  generateAndSaveExamReportApi,
  getExamReportByOrderApi,
  pendingExamOrdersApi,
  saveExamReportApi,
  type ExamOrder,
} from '@/api/exam'

const queue = ref<ExamOrder[]>([])
const loading = ref(false)
const reportDialog = ref(false)
const currentOrder = ref<ExamOrder | null>(null)
const currentReportId = ref<number | null>(null)
const saving = ref(false)
const generating = ref(false)
const publishing = ref(false)

const reportForm = reactive({
  reportText: '',
  conclusion: '',
})

async function loadQueue() {
  loading.value = true
  try {
    queue.value = await pendingExamOrdersApi()
  } finally {
    loading.value = false
  }
}

async function openReport(row: ExamOrder) {
  currentOrder.value = row
  currentReportId.value = null
  reportForm.reportText = ''
  reportForm.conclusion = ''
  reportDialog.value = true

  const existing = await getExamReportByOrderApi(row.id)
  if (existing) {
    reportForm.reportText = existing.reportText || ''
    reportForm.conclusion = existing.conclusion || ''
    currentReportId.value = existing.id
  }
}

async function onGenerate() {
  if (!currentOrder.value) return
  generating.value = true
  try {
    const report = await generateAndSaveExamReportApi(currentOrder.value.id)
    reportForm.reportText = report.reportText || ''
    reportForm.conclusion = report.conclusion || ''
    currentReportId.value = report.id
    ElMessage.success('检查报告已生成并保存，请核对后确认发布')
  } finally {
    generating.value = false
  }
}

async function onSaveDraft() {
  if (!currentOrder.value) return
  if (!reportForm.reportText.trim() || !reportForm.conclusion.trim()) {
    ElMessage.warning('请填写检查所见和结论')
    return
  }
  saving.value = true
  try {
    const saved = await saveExamReportApi({
      orderId: currentOrder.value.id,
      reportText: reportForm.reportText,
      conclusion: reportForm.conclusion,
    })
    currentReportId.value = saved.id
    ElMessage.success('报告已保存')
  } finally {
    saving.value = false
  }
}

async function onConfirmPublish() {
  if (!currentOrder.value) return
  if (!reportForm.reportText.trim() || !reportForm.conclusion.trim()) {
    ElMessage.warning('请先生成或填写报告内容')
    return
  }
  publishing.value = true
  try {
    const saved = await saveExamReportApi({
      orderId: currentOrder.value.id,
      reportText: reportForm.reportText,
      conclusion: reportForm.conclusion,
    })
    await confirmExamReportApi(saved.id)
    ElMessage.success('报告已确认发布，检查完成')
    reportDialog.value = false
    await loadQueue()
  } finally {
    publishing.value = false
  }
}

onMounted(loadQueue)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>待检查列表</span>
          <el-button type="primary" :loading="loading" @click="loadQueue">刷新</el-button>
        </div>
      </template>
      <el-alert
        title="仅显示患者已缴费的检查单。流程：一键生成报告 → 核对修改 → 确认发布"
        type="info"
        :closable="false"
        show-icon
        class="tip"
      />
      <el-table v-loading="loading" :data="queue" stripe>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column label="类型" width="70">
          <template #default="{ row }: { row: ExamOrder }">
            {{ row.orderType === 'LAB' ? '检验' : '检查' }}
          </template>
        </el-table-column>
        <el-table-column prop="examType" label="项目" width="120" />
        <el-table-column prop="bodyPart" label="部位/类别" width="100" />
        <el-table-column prop="clinicalIndication" label="临床指征" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }: { row: ExamOrder }">
            <el-button type="primary" link @click="openReport(row)">进行检查</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && queue.length === 0" description="暂无待检查项目（需患者先缴费）" />
    </el-card>

    <el-dialog v-model="reportDialog" title="检查报告" width="640px" destroy-on-close>
      <template v-if="currentOrder">
        <p class="order-info">
          {{ currentOrder.patientName }} · {{ currentOrder.examType }}
          <span v-if="currentOrder.bodyPart"> · {{ currentOrder.bodyPart }}</span>
        </p>
        <el-button type="primary" :loading="generating" class="gen-btn" @click="onGenerate">
          一键生成报告
        </el-button>
        <el-tag v-if="currentReportId" type="success" size="small" class="ai-tag">已生成（AI）</el-tag>
        <el-form label-position="top">
          <el-form-item label="检查所见">
            <el-input v-model="reportForm.reportText" type="textarea" :rows="6" placeholder="含参考值的检查描述" />
          </el-form-item>
          <el-form-item label="结论">
            <el-input v-model="reportForm.conclusion" type="textarea" :rows="3" placeholder="诊断结论" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button :loading="saving" @click="onSaveDraft">保存草稿</el-button>
        <el-button type="success" :loading="publishing" @click="onConfirmPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { padding: 8px 0; }
.tip { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.order-info { margin: 0 0 12px; color: #606266; font-weight: 500; }
.gen-btn { margin-right: 8px; margin-bottom: 12px; }
.ai-tag { margin-bottom: 12px; vertical-align: middle; }
</style>
