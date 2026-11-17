<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { inspectorExamHistoryApi, type InspectorExamHistory } from '@/api/exam'

const loading = ref(false)
const records = ref<InspectorExamHistory[]>([])
const patientName = ref('')
const doctorName = ref('')
const searchDate = ref<string>()

const detailVisible = ref(false)
const currentRecord = ref<InspectorExamHistory | null>(null)

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function orderTypeLabel(type?: string) {
  return type === 'LAB' ? '检验' : '检查'
}

async function loadRecords() {
  loading.value = true
  try {
    const params: { patientName?: string; doctorName?: string; date?: string } = {}
    const patient = patientName.value.trim()
    const doctor = doctorName.value.trim()
    if (patient) params.patientName = patient
    if (doctor) params.doctorName = doctor
    if (searchDate.value) params.date = searchDate.value
    records.value = await inspectorExamHistoryApi(params)
  } catch {
    ElMessage.error('加载历史检查记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadRecords()
}

function handleReset() {
  patientName.value = ''
  doctorName.value = ''
  searchDate.value = undefined
  loadRecords()
}

function openDetail(row: InspectorExamHistory) {
  currentRecord.value = row
  detailVisible.value = true
}

onMounted(loadRecords)
</script>

<template>
  <div class="history-page">
    <div class="page-header">
      <h2 class="page-title">历史检查记录</h2>
      <p class="page-desc">查看已完成的检查/检验及报告，含患者、开单医生与检查内容</p>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="患者姓名">
          <el-input v-model="patientName" placeholder="模糊搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="开单医生">
          <el-input v-model="doctorName" placeholder="模糊搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="完成日期">
          <el-date-picker
            v-model="searchDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadRecords">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="records" stripe border>
        <el-table-column label="完成时间" width="150">
          <template #default="{ row }: { row: InspectorExamHistory }">
            {{ formatDateTime(row.confirmedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="开单医生" width="100" />
        <el-table-column label="类型" width="70">
          <template #default="{ row }: { row: InspectorExamHistory }">
            {{ orderTypeLabel(row.orderType) }}
          </template>
        </el-table-column>
        <el-table-column prop="examType" label="检查项目" min-width="120" />
        <el-table-column prop="bodyPart" label="部位/类别" width="100" />
        <el-table-column prop="inspectorName" label="检查医师" width="100" />
        <el-table-column label="结论摘要" min-width="160" show-overflow-tooltip>
          <template #default="{ row }: { row: InspectorExamHistory }">
            {{ row.conclusion || row.reportText || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }: { row: InspectorExamHistory }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无历史检查记录" />
    </el-card>

    <el-drawer v-model="detailVisible" title="检查报告详情" size="560px" destroy-on-close>
      <template v-if="currentRecord">
        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="患者">{{ currentRecord.patientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开单医生">{{ currentRecord.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查类型">
            {{ orderTypeLabel(currentRecord.orderType) }}
          </el-descriptions-item>
          <el-descriptions-item label="检查项目">{{ currentRecord.examType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部位/类别">{{ currentRecord.bodyPart || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查医师">{{ currentRecord.inspectorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开单时间">
            {{ formatDateTime(currentRecord.orderCreatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="报告发布时间">
            {{ formatDateTime(currentRecord.confirmedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="临床指征" :span="2">
            {{ currentRecord.clinicalIndication || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <section class="report-block">
          <div class="block-title">检查所见 / 数据</div>
          <div class="block-content">{{ currentRecord.reportText || '暂无' }}</div>
        </section>
        <section class="report-block">
          <div class="block-title">结论</div>
          <div class="block-content conclusion">{{ currentRecord.conclusion || '暂无' }}</div>
        </section>
        <el-tag v-if="currentRecord.isAiGenerated === 1" type="success" size="small">AI 辅助生成</el-tag>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.history-page {
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

.search-card {
  margin-bottom: 16px;
}

.detail-desc {
  margin-bottom: 20px;
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
}
</style>
