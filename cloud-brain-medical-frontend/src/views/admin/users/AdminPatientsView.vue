<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  adminPatientDetailApi,
  adminPatientListApi,
  adminPatientStatsApi,
  updateAdminPatientApi,
  type AdminPatientDetail,
  type AdminPatientListItem,
  type AdminPatientStats,
} from '@/api/adminPatient'
import AdminPagination from '@/components/AdminPagination.vue'
import type { Registration, RegistrationStatus } from '@/types/api'

const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const stats = ref<AdminPatientStats | null>(null)
const patients = ref<AdminPatientListItem[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const detailVisible = ref(false)
const detailTab = ref('profile')
const currentPatient = ref<AdminPatientDetail | null>(null)

const filters = reactive({
  keyword: '',
  gender: undefined as number | undefined,
  hasAllergy: undefined as boolean | undefined,
  registerDateRange: [] as string[],
})

const editForm = reactive({
  realName: '',
  phone: '',
  gender: undefined as number | undefined,
  birthDate: '',
  idCard: '',
  bloodType: '',
  allergyHistory: '',
  medicalHistory: '',
  emergencyContact: '',
  emergencyPhone: '',
  status: 1,
})

const regStatusLabel: Record<RegistrationStatus, string> = {
  PENDING: '待就诊',
  IN_PROGRESS: '就诊中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const regStatusType: Record<RegistrationStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  PENDING: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
}

const statCards = computed(() => {
  if (!stats.value) return []
  return [
    { label: '患者总数', value: stats.value.totalCount, color: '#409eff' },
    { label: '今日新增', value: stats.value.todayNewCount, color: '#67c23a' },
    { label: '有过敏史', value: stats.value.allergyCount, color: '#e6a23c' },
    { label: '已就诊', value: stats.value.visitedCount, color: '#909399' },
    { label: '男性', value: stats.value.maleCount, color: '#337ecc' },
    { label: '女性', value: stats.value.femaleCount, color: '#f56c6c' },
    { label: '正常账号', value: stats.value.activeCount, color: '#13c2c2' },
  ]
})

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function formatDate(value?: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

function timeSlotLabel(slot?: string) {
  if (slot === 'MORNING') return '上午'
  if (slot === 'AFTERNOON') return '下午'
  return slot || '-'
}

async function loadStats() {
  stats.value = await adminPatientStatsApi()
}

async function loadPatients() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.gender !== undefined) params.gender = filters.gender
    if (filters.hasAllergy !== undefined) params.hasAllergy = filters.hasAllergy
    if (filters.registerDateRange?.length === 2) {
      params.registerDateFrom = filters.registerDateRange[0]
      params.registerDateTo = filters.registerDateRange[1]
    }
    params.page = pagination.page
    params.size = pagination.size
    const result = await adminPatientListApi(params)
    patients.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadStats(), loadPatients()])
}

function resetFilters() {
  filters.keyword = ''
  filters.gender = undefined
  filters.hasAllergy = undefined
  filters.registerDateRange = []
  pagination.page = 1
  loadPatients()
}

function fillEditForm(detail: AdminPatientDetail) {
  editForm.realName = detail.realName
  editForm.phone = detail.phone
  editForm.gender = detail.gender
  editForm.birthDate = detail.birthDate || ''
  editForm.idCard = detail.idCard || ''
  editForm.bloodType = detail.bloodType || ''
  editForm.allergyHistory = detail.allergyHistory || ''
  editForm.medicalHistory = detail.medicalHistory || ''
  editForm.emergencyContact = detail.emergencyContact || ''
  editForm.emergencyPhone = detail.emergencyPhone || ''
  editForm.status = detail.status
}

async function openDetail(row: AdminPatientListItem) {
  detailVisible.value = true
  detailTab.value = 'profile'
  detailLoading.value = true
  try {
    currentPatient.value = await adminPatientDetailApi(row.id)
    if (currentPatient.value) {
      fillEditForm(currentPatient.value)
    }
  } finally {
    detailLoading.value = false
  }
}

async function saveProfile() {
  if (!currentPatient.value) return
  saving.value = true
  try {
    currentPatient.value = await updateAdminPatientApi(currentPatient.value.id, {
      realName: editForm.realName,
      phone: editForm.phone,
      gender: editForm.gender,
      birthDate: editForm.birthDate || undefined,
      idCard: editForm.idCard,
      bloodType: editForm.bloodType,
      allergyHistory: editForm.allergyHistory,
      medicalHistory: editForm.medicalHistory,
      emergencyContact: editForm.emergencyContact,
      emergencyPhone: editForm.emergencyPhone,
      status: editForm.status,
    })
    ElMessage.success('患者档案已更新')
    await loadAll()
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="page">
    <el-row :gutter="16" class="stats-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="6" :lg="3">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>患者列表</span>
          <el-button type="primary" :loading="loading" @click="loadAll">刷新</el-button>
        </div>
      </template>

      <el-form inline class="filters">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="姓名/用户名/手机号"
            clearable
            style="width: 180px"
            @keyup.enter="loadPatients"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="filters.gender" placeholder="全部" clearable style="width: 100px">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="过敏史">
          <el-select v-model="filters.hasAllergy" placeholder="全部" clearable style="width: 100px">
            <el-option label="有" :value="true" />
            <el-option label="无" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="filters.registerDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { pagination.page = 1; loadPatients() }">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="patients" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="genderLabel" label="性别" width="70" />
        <el-table-column label="过敏史" min-width="120" show-overflow-tooltip>
          <template #default="{ row }: { row: AdminPatientListItem }">
            {{ row.allergyHistory || '无' }}
          </template>
        </el-table-column>
        <el-table-column prop="registrationCount" label="挂号次数" width="90" align="center" />
        <el-table-column prop="medicalRecordCount" label="病历数" width="80" align="center" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }: { row: AdminPatientListItem }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="150">
          <template #default="{ row }: { row: AdminPatientListItem }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }: { row: AdminPatientListItem }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AdminPagination
        v-model:page="pagination.page"
        v-model:size="pagination.size"
        :total="pagination.total"
        @change="loadPatients"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="患者详情" size="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="currentPatient">
          <el-tabs v-model="detailTab">
            <el-tab-pane label="基本档案" name="profile">
              <el-form :model="editForm" label-width="100px" class="detail-form">
                <el-row :gutter="16">
                  <el-col :span="12">
                    <el-form-item label="用户名">
                      <el-input :model-value="currentPatient.username" disabled />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="姓名">
                      <el-input v-model="editForm.realName" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="手机号">
                      <el-input v-model="editForm.phone" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="性别">
                      <el-select v-model="editForm.gender" placeholder="请选择" style="width: 100%">
                        <el-option label="男" :value="1" />
                        <el-option label="女" :value="2" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="出生日期">
                      <el-date-picker
                        v-model="editForm.birthDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="血型">
                      <el-input v-model="editForm.bloodType" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="身份证号">
                      <el-input v-model="editForm.idCard" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="账号状态">
                      <el-select v-model="editForm.status" style="width: 100%">
                        <el-option label="正常" :value="1" />
                        <el-option label="禁用" :value="0" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="过敏史">
                      <el-input v-model="editForm.allergyHistory" type="textarea" :rows="2" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="既往史">
                      <el-input v-model="editForm.medicalHistory" type="textarea" :rows="2" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="紧急联系人">
                      <el-input v-model="editForm.emergencyContact" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="紧急电话">
                      <el-input v-model="editForm.emergencyPhone" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="注册时间">
                      <span>{{ formatDateTime(currentPatient.createdAt) }}</span>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存档案</el-button>
              </el-form>
            </el-tab-pane>

            <el-tab-pane :label="`挂号信息 (${currentPatient.registrations.length})`" name="registrations">
              <el-table :data="currentPatient.registrations" stripe max-height="480">
                <el-table-column prop="regNo" label="挂号单号" width="140" />
                <el-table-column prop="deptName" label="科室" width="100" />
                <el-table-column prop="doctorName" label="医生" width="90" />
                <el-table-column label="就诊日" width="110">
                  <template #default="{ row }: { row: Registration }">
                    {{ formatDate(row.workDate) }}
                  </template>
                </el-table-column>
                <el-table-column label="时段" width="70">
                  <template #default="{ row }: { row: Registration }">
                    {{ timeSlotLabel(row.timeSlot) }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }: { row: Registration }">
                    <el-tag :type="regStatusType[row.status]" size="small">
                      {{ regStatusLabel[row.status] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="缴费" width="70">
                  <template #default="{ row }: { row: Registration }">
                    <el-tag :type="row.paid ? 'success' : 'warning'" size="small">
                      {{ row.paid ? '已缴' : '未缴' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="chiefComplaint" label="主诉" min-width="120" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`就诊记录 (${currentPatient.medicalRecords.length})`" name="records">
              <el-collapse accordion>
                <el-collapse-item
                  v-for="record in currentPatient.medicalRecords"
                  :key="record.id"
                  :title="`${formatDateTime(record.createdAt)} · ${record.doctorName} · ${record.diagnosis || '待诊断'}`"
                >
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item label="主诉">{{ record.chiefComplaint || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="现病史">{{ record.presentIllness || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="既往史">{{ record.pastHistory || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="体格检查">{{ record.physicalExam || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="诊断">{{ record.diagnosis || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="治疗方案">{{ record.treatmentPlan || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                      {{ record.status === 'CONFIRMED' ? '已确认' : '草稿' }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-collapse-item>
              </el-collapse>
              <el-empty v-if="!currentPatient.medicalRecords.length" description="暂无就诊记录" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filters {
  margin-bottom: 16px;
}

.detail-form {
  padding-right: 8px;
}
</style>
