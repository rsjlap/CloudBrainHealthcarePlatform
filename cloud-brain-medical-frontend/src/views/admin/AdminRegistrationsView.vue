<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminPagination from '@/components/AdminPagination.vue'
import { listDepartmentsApi } from '@/api/department'
import { adminDoctorsApi, type AdminDoctor } from '@/api/adminDoctor'
import { adminRegistrationsApi } from '@/api/adminRegistration'
import type { Department, Registration, RegistrationStatus } from '@/types/api'

const loading = ref(false)
const registrations = ref<Registration[]>([])
const departments = ref<Department[]>([])
const doctors = ref<AdminDoctor[]>([])

const pagination = reactive({ page: 1, size: 10, total: 0 })

const filters = reactive({
  keyword: '',
  status: '' as RegistrationStatus | '',
  departmentId: undefined as number | undefined,
  doctorId: undefined as number | undefined,
  paid: undefined as boolean | undefined,
  dateRange: [] as string[],
})

const statusOptions: { value: RegistrationStatus; label: string }[] = [
  { value: 'PENDING', label: '待就诊' },
  { value: 'IN_PROGRESS', label: '就诊中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

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

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function timeSlotLabel(slot?: string) {
  if (slot === 'MORNING') return '上午'
  if (slot === 'AFTERNOON') return '下午'
  if (slot === 'EVENING') return '晚上'
  return slot || '-'
}

async function loadDepartments() {
  departments.value = await listDepartmentsApi()
}

async function loadDoctors() {
  if (!filters.departmentId) {
    doctors.value = []
    filters.doctorId = undefined
    return
  }
  const result = await adminDoctorsApi({
    departmentId: filters.departmentId,
    page: 1,
    size: 200,
    status: 1,
  })
  doctors.value = result.records
  if (filters.doctorId && !doctors.value.some((d) => d.id === filters.doctorId)) {
    filters.doctorId = undefined
  }
}

async function loadRegistrations() {
  loading.value = true
  try {
    const result = await adminRegistrationsApi({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      departmentId: filters.departmentId,
      doctorId: filters.doctorId,
      paid: filters.paid,
      dateFrom: filters.dateRange?.[0],
      dateTo: filters.dateRange?.[1],
    })
    registrations.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.departmentId = undefined
  filters.doctorId = undefined
  filters.paid = undefined
  filters.dateRange = []
  doctors.value = []
  pagination.page = 1
  loadRegistrations()
}

function onDepartmentChange() {
  filters.doctorId = undefined
  loadDoctors()
}

onMounted(async () => {
  await loadDepartments()
  await loadRegistrations()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>挂号记录</span>
          <el-button type="primary" :loading="loading" @click="loadRegistrations">刷新</el-button>
        </div>
      </template>

      <el-form inline class="filters">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="单号/患者姓名"
            clearable
            style="width: 160px"
            @keyup.enter="loadRegistrations"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 110px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科室">
          <el-select
            v-model="filters.departmentId"
            placeholder="全部"
            clearable
            style="width: 130px"
            @change="onDepartmentChange"
          >
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生">
          <el-select
            v-model="filters.doctorId"
            placeholder="全部"
            clearable
            style="width: 130px"
            :disabled="!filters.departmentId"
          >
            <el-option v-for="d in doctors" :key="d.id" :label="d.realName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费">
          <el-select v-model="filters.paid" placeholder="全部" clearable style="width: 100px">
            <el-option label="已缴" :value="true" />
            <el-option label="未缴" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊日期">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadRegistrations">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="registrations" stripe>
        <el-table-column prop="regNo" label="挂号单号" width="140" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="workDate" label="就诊日" width="110" />
        <el-table-column label="时段" width="70">
          <template #default="{ row }: { row: Registration }">
            {{ timeSlotLabel(row.timeSlot) }}
          </template>
        </el-table-column>
        <el-table-column prop="queueNo" label="排队号" width="80" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: Registration }">
            <el-tag :type="regStatusType[row.status]" size="small">
              {{ regStatusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缴费" width="80">
          <template #default="{ row }: { row: Registration }">
            <el-tag :type="row.paid ? 'success' : 'warning'" size="small">
              {{ row.paid ? '已缴' : '未缴' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }: { row: Registration }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:page="pagination.page"
        v-model:size="pagination.size"
        :total="pagination.total"
        @change="loadRegistrations"
      />
    </el-card>
  </div>
</template>

<style scoped>
.page {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filters {
  margin-bottom: 16px;
}
</style>
