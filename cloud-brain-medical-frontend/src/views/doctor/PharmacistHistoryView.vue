<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { dispensedPrescriptionsApi, getPrescriptionApi } from '@/api/prescription'
import type { Prescription, RiskLevel } from '@/types/api'
import PharmacistPrescriptionPanel from './components/PharmacistPrescriptionPanel.vue'

const loading = ref(false)
const records = ref<Prescription[]>([])
const patientName = ref('')
const doctorName = ref('')
const searchDate = ref<string>()

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

async function loadRecords() {
  loading.value = true
  try {
    const params: { patientName?: string; doctorName?: string; date?: string } = {}
    const patient = patientName.value.trim()
    const doctor = doctorName.value.trim()
    if (patient) params.patientName = patient
    if (doctor) params.doctorName = doctor
    if (searchDate.value) params.date = searchDate.value
    records.value = await dispensedPrescriptionsApi(params)
  } catch {
    ElMessage.error('加载发药记录失败')
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

onMounted(loadRecords)
</script>

<template>
  <div class="history-page">
    <div class="page-header">
      <h2 class="page-title">发药记录</h2>
      <p class="page-desc">查看已发药处方历史，含患者、医生及 AI 审核信息</p>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form inline @submit.prevent="handleSearch">
        <el-form-item label="患者姓名">
          <el-input v-model="patientName" placeholder="模糊搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="开方医生">
          <el-input v-model="doctorName" placeholder="模糊搜索" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="发药日期">
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
        <el-table-column label="发药时间" width="150">
          <template #default="{ row }: { row: Prescription }">
            {{ formatDateTime(row.dispensedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="开方医生" width="100" />
        <el-table-column prop="dispensedByName" label="发药药师" width="100" />
        <el-table-column label="AI 风险" width="100">
          <template #default="{ row }: { row: Prescription }">
            <el-tag :type="riskTagType(row.riskLevel)" size="small">
              {{ riskLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="药品" min-width="160" show-overflow-tooltip>
          <template #default="{ row }: { row: Prescription }">
            {{ row.items.map((i) => i.drugName).join('、') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }: { row: Prescription }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && records.length === 0" description="暂无发药记录" />
    </el-card>

    <el-drawer v-model="detailVisible" title="处方详情" size="580px" destroy-on-close>
      <div v-loading="detailLoading">
        <PharmacistPrescriptionPanel :prescription="currentPrescription" />
      </div>
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
</style>
