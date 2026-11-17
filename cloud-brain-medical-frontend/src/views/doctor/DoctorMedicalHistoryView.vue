<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { doctorMedicalRecordsApi } from '@/api/medicalRecord'
import { listExamOrdersByRegistrationApi, getExamReportByOrderApi, type ExamOrder, type ExamReport } from '@/api/exam'
import { listPrescriptionsByRegistrationApi } from '@/api/prescription'
import type { MedicalRecord, Prescription } from '@/types/api'
import ExamReportDetailDialog from './components/ExamReportDetailDialog.vue'

const loading = ref(false)
const detailLoading = ref(false)
const records = ref<MedicalRecord[]>([])
const patientName = ref('')
const searchDate = ref<string>()

const detailVisible = ref(false)
const currentRecord = ref<MedicalRecord | null>(null)
const examOrders = ref<ExamOrder[]>([])
const examReports = ref<Record<number, ExamReport>>({})
const examReportDialogVisible = ref(false)
const viewingExamOrder = ref<ExamOrder | null>(null)
const viewingExamReport = ref<ExamReport | null>(null)
const prescriptions = ref<Prescription[]>([])
const detailTab = ref('record')

const examStatusLabel: Record<string, string> = {
  PENDING: '待检查',
  IN_PROGRESS: '检查中',
  DONE: '已完成',
}

const prescriptionStatusLabel: Record<string, string> = {
  PENDING_DISPENSE: '待发药',
  DISPENSED: '已发药',
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function visitDate(record: MedicalRecord) {
  return formatDateTime(record.confirmedAt || record.createdAt).slice(0, 10)
}

async function loadRecords() {
  loading.value = true
  try {
    const params: { patientName?: string; date?: string } = {}
    const name = patientName.value.trim()
    if (name) params.patientName = name
    if (searchDate.value) params.date = searchDate.value
    records.value = await doctorMedicalRecordsApi(params)
  } catch {
    ElMessage.error('加载接诊记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadRecords()
}

function handleReset() {
  patientName.value = ''
  searchDate.value = undefined
  loadRecords()
}

async function openDetail(record: MedicalRecord) {
  currentRecord.value = record
  detailTab.value = 'record'
  detailVisible.value = true
  detailLoading.value = true
  examOrders.value = []
  examReports.value = {}
  prescriptions.value = []
  try {
    const [orders, rxList] = await Promise.all([
      listExamOrdersByRegistrationApi(record.registrationId),
      listPrescriptionsByRegistrationApi(record.registrationId),
    ])
    examOrders.value = orders
    prescriptions.value = rxList

    const reports: Record<number, ExamReport> = {}
    for (const order of orders) {
      if (order.status === 'DONE') {
        const report = await getExamReportByOrderApi(order.id)
        if (report) reports[order.id] = report
      }
    }
    examReports.value = reports
  } catch {
    ElMessage.error('加载病历详情失败')
  } finally {
    detailLoading.value = false
  }
}

function openExamReport(order: ExamOrder) {
  const report = examReports.value[order.id]
  if (!report) return
  viewingExamOrder.value = order
  viewingExamReport.value = report
  examReportDialogVisible.value = true
}

onMounted(loadRecords)
</script>

<template>
  <div class="history-page">
    <div class="page-header">
      <h2 class="page-title">我的接诊病历</h2>
      <p class="page-desc">查看历史接诊记录及开出的检查检验与处方</p>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="患者姓名">
          <el-input
            v-model="patientName"
            placeholder="输入患者姓名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="就诊日期">
          <el-date-picker
            v-model="searchDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadRecords">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="records" stripe border>
        <el-table-column prop="patientName" label="患者姓名" width="120" />
        <el-table-column label="就诊日期" width="120">
          <template #default="{ row }">{{ visitDate(row) }}</template>
        </el-table-column>
        <el-table-column prop="chiefComplaint" label="主诉" min-width="160" show-overflow-tooltip />
        <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
        <el-table-column label="确认时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.confirmedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无接诊记录" />
    </el-card>

    <el-drawer
      v-model="detailVisible"
      :title="`${currentRecord?.patientName ?? ''} · 接诊详情`"
      size="640px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-tabs v-model="detailTab">
          <el-tab-pane label="病历摘要" name="record">
            <el-descriptions v-if="currentRecord" :column="1" border size="small">
              <el-descriptions-item label="患者">{{ currentRecord.patientName }}</el-descriptions-item>
              <el-descriptions-item label="就诊日期">{{ visitDate(currentRecord) }}</el-descriptions-item>
              <el-descriptions-item label="主诉">{{ currentRecord.chiefComplaint || '-' }}</el-descriptions-item>
              <el-descriptions-item label="现病史">{{ currentRecord.presentIllness || '-' }}</el-descriptions-item>
              <el-descriptions-item label="既往史">{{ currentRecord.pastHistory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="体格检查">{{ currentRecord.physicalExam || '-' }}</el-descriptions-item>
              <el-descriptions-item label="诊断">{{ currentRecord.diagnosis || '-' }}</el-descriptions-item>
              <el-descriptions-item label="治疗方案">{{ currentRecord.treatmentPlan || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="检查检验" name="exam">
            <el-table :data="examOrders" size="small" border stripe>
              <el-table-column label="项目" width="120">
                <template #default="{ row }: { row: ExamOrder }">
                  <el-button
                    v-if="examReports[row.id]"
                    type="primary"
                    link
                    @click="openExamReport(row)"
                  >
                    {{ row.examType }}
                  </el-button>
                  <span v-else>{{ row.examType }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="bodyPart" label="部位" width="80" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  {{ examStatusLabel[row.status] || row.status }}
                </template>
              </el-table-column>
              <el-table-column label="结论" min-width="160">
                <template #default="{ row }: { row: ExamOrder }">
                  <template v-if="examReports[row.id]">
                    <span class="report-preview">{{ examReports[row.id].conclusion || '—' }}</span>
                    <el-button type="primary" link @click="openExamReport(row)">查看详情</el-button>
                  </template>
                  <span v-else-if="row.status === 'DONE'" class="muted">暂无报告</span>
                  <span v-else class="muted">待出结果</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="examOrders.length === 0" description="未开具检查检验" />
          </el-tab-pane>

          <el-tab-pane label="处方" name="rx">
            <div v-for="rx in prescriptions" :key="rx.id" class="rx-block">
              <div class="rx-header">
                <span>处方 #{{ rx.id }}</span>
                <el-tag size="small" :type="rx.status === 'DISPENSED' ? 'success' : 'warning'">
                  {{ prescriptionStatusLabel[rx.status] || rx.status }}
                </el-tag>
                <span class="rx-time">{{ formatDateTime(rx.createdAt) }}</span>
              </div>
              <el-table :data="rx.items" size="small" border>
                <el-table-column prop="drugName" label="药品" />
                <el-table-column label="用量" width="100">
                  <template #default="{ row }">
                    {{ row.dosage }}{{ row.unit }}
                  </template>
                </el-table-column>
                <el-table-column prop="frequency" label="频次" width="80" />
                <el-table-column prop="durationDays" label="天数" width="60" />
              </el-table>
            </div>
            <el-empty v-if="prescriptions.length === 0" description="未开具处方" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <ExamReportDetailDialog
      v-model="examReportDialogVisible"
      :order="viewingExamOrder"
      :report="viewingExamReport"
    />
  </div>
</template>

<style scoped>
.history-page {
  padding: 20px;
  height: 100%;
  overflow: auto;
  box-sizing: border-box;
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

.search-card {
  margin-bottom: 16px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 2px;
}

.table-card :deep(.el-card__body) {
  padding: 16px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.report-preview {
  display: inline-block;
  max-width: 180px;
  margin-right: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #606266;
  font-size: 12px;
}

.rx-block {
  margin-bottom: 16px;
}

.rx-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.rx-time {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}
</style>
