<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listDepartmentsApi } from '@/api/department'
import { listDoctorsApi } from '@/api/doctor'
import {
  adminSchedulesAllApi,
  createAdminScheduleApi,
  updateAdminScheduleApi,
  type AdminSchedule,
  type TimeSlot,
} from '@/api/adminSchedule'
import AdminAiSchedulePanel from '@/components/AdminAiSchedulePanel.vue'
import type { Department, DoctorCard } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const viewMode = ref<'week' | 'ai'>('week')
const departments = ref<Department[]>([])
const doctors = ref<DoctorCard[]>([])
const schedules = ref<AdminSchedule[]>([])
const dialogVisible = ref(false)
const editing = ref<AdminSchedule | null>(null)
const formRef = ref<FormInstance>()

const filters = reactive({
  departmentId: undefined as number | undefined,
})

const form = reactive({
  doctorId: undefined as number | undefined,
  workDate: '',
  timeSlot: 'MORNING' as TimeSlot,
  maxPatients: 20,
})

const timeSlotOptions = [
  { value: 'MORNING', label: '上午' },
  { value: 'AFTERNOON', label: '下午' },
  { value: 'EVENING', label: '晚上' },
]

const rules: FormRules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择班次', trigger: 'change' }],
  maxPatients: [{ required: true, message: '请设置号源上限', trigger: 'blur' }],
}

const dialogTitle = computed(() => (editing.value ? '编辑排班' : '新增排班'))
const canEditDoctorFields = computed(() => !editing.value || editing.value.registeredCount === 0)

function getMonday(date = new Date()) {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  return d.toISOString().slice(0, 10)
}

function addDays(dateStr: string, days: number) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

const weekStart = ref(getMonday())

const weekDays = computed(() => {
  const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return labels.map((label, index) => {
    const date = addDays(weekStart.value, index)
    return { label, date, short: date.slice(5) }
  })
})

const weekEnd = computed(() => addDays(weekStart.value, 6))

const weekSchedules = computed(() =>
  schedules.value.filter(
    (s) => s.workDate >= weekStart.value && s.workDate <= weekEnd.value,
  ),
)

function schedulesInCell(date: string, slot: TimeSlot) {
  return weekSchedules.value.filter((s) => s.workDate === date && s.timeSlot === slot)
}

function slotProgress(row: AdminSchedule) {
  const pct = row.maxPatients ? Math.round((row.registeredCount / row.maxPatients) * 100) : 0
  return Math.min(pct, 100)
}

function prevWeek() {
  weekStart.value = addDays(weekStart.value, -7)
}

function nextWeek() {
  weekStart.value = addDays(weekStart.value, 7)
}

function goCurrentWeek() {
  weekStart.value = getMonday()
}

async function loadDepartments() {
  departments.value = await listDepartmentsApi()
}

async function loadDoctors() {
  if (!filters.departmentId && !form.doctorId) {
    doctors.value = []
    return
  }
  const deptId = filters.departmentId || departments.value[0]?.id
  if (!deptId) return
  doctors.value = await listDoctorsApi(deptId)
}

async function loadSchedules() {
  loading.value = true
  try {
    schedules.value = await adminSchedulesAllApi({
      departmentId: filters.departmentId,
    })
  } finally {
    loading.value = false
  }
}

function openCreateFromCell(date: string, slot: TimeSlot) {
  editing.value = null
  resetForm()
  form.workDate = date
  form.timeSlot = slot
  if (filters.departmentId) {
    loadDoctors()
  }
  dialogVisible.value = true
}

function onCellClick(date: string, slot: TimeSlot) {
  const items = schedulesInCell(date, slot)
  if (items.length === 1) {
    openEdit(items[0]!)
    return
  }
  if (items.length === 0) {
    openCreateFromCell(date, slot)
  }
}

function onViewModeChange() {
  if (viewMode.value === 'week') {
    loadSchedules()
  }
}

function resetForm() {
  form.doctorId = undefined
  form.workDate = new Date().toISOString().slice(0, 10)
  form.timeSlot = 'MORNING'
  form.maxPatients = 20
}

function openCreate() {
  editing.value = null
  resetForm()
  if (filters.departmentId) {
    loadDoctors()
  }
  dialogVisible.value = true
}

function openEdit(row: AdminSchedule) {
  editing.value = row
  form.doctorId = row.doctorId
  form.workDate = row.workDate
  form.timeSlot = row.timeSlot
  form.maxPatients = row.maxPatients
  if (row.departmentId) {
    filters.departmentId = row.departmentId
    loadDoctors()
  }
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateAdminScheduleApi(editing.value.id, {
        doctorId: canEditDoctorFields.value ? form.doctorId : undefined,
        workDate: canEditDoctorFields.value ? form.workDate : undefined,
        timeSlot: canEditDoctorFields.value ? form.timeSlot : undefined,
        maxPatients: form.maxPatients,
      })
      ElMessage.success('排班已更新')
    } else {
      await createAdminScheduleApi({
        doctorId: form.doctorId!,
        workDate: form.workDate,
        timeSlot: form.timeSlot,
        maxPatients: form.maxPatients,
      })
      ElMessage.success('排班已新增')
    }
    dialogVisible.value = false
    await loadSchedules()
  } finally {
    saving.value = false
  }
}

function onFilterDepartmentChange() {
  loadDoctors()
  if (viewMode.value === 'week') {
    loadSchedules()
  }
}

onMounted(async () => {
  await loadDepartments()
  if (departments.value.length > 0) {
    filters.departmentId = departments.value[0].id
    await loadDoctors()
  }
  await loadSchedules()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>医生排班管理</span>
          <div class="header-actions">
            <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">
              <el-radio-button value="week">周视图</el-radio-button>
              <el-radio-button value="ai">AI 智能排班</el-radio-button>
            </el-radio-group>
            <el-button v-if="viewMode === 'week'" type="primary" @click="openCreate">新增排班</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filters">
        <el-form-item label="科室">
          <el-select
            v-model="filters.departmentId"
            placeholder="全部科室"
            clearable
            style="width: 160px"
            @change="onFilterDepartmentChange"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <template v-if="viewMode === 'week'">
          <el-form-item label="周次">
            <el-button size="small" @click="prevWeek">上一周</el-button>
            <span class="week-range">{{ weekStart }} ~ {{ weekEnd }}</span>
            <el-button size="small" @click="nextWeek">下一周</el-button>
            <el-button size="small" text type="primary" @click="goCurrentWeek">本周</el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="loadSchedules">查询</el-button>
          </el-form-item>
        </template>
      </el-form>

      <div v-if="viewMode === 'week'" v-loading="loading" class="week-grid-wrap">
        <div class="week-grid">
          <div class="week-corner" />
          <div v-for="day in weekDays" :key="day.date" class="week-day-head">
            <div>{{ day.label }}</div>
            <div class="day-date">{{ day.short }}</div>
          </div>
          <template v-for="slot in timeSlotOptions" :key="slot.value">
            <div class="slot-label">{{ slot.label }}</div>
            <div
              v-for="day in weekDays"
              :key="`${slot.value}-${day.date}`"
              class="week-cell"
              @click="onCellClick(day.date, slot.value as TimeSlot)"
            >
              <div
                v-for="item in schedulesInCell(day.date, slot.value as TimeSlot)"
                :key="item.id"
                class="schedule-chip"
                :class="{ full: item.remainingSlots <= 0 }"
                @click.stop="openEdit(item)"
              >
                <div class="chip-name">{{ item.doctorName }}</div>
                <div class="chip-sub">{{ item.registeredCount }}/{{ item.maxPatients }}</div>
                <el-progress
                  :percentage="slotProgress(item)"
                  :stroke-width="4"
                  :show-text="false"
                  :status="item.remainingSlots <= 0 ? 'exception' : undefined"
                />
              </div>
              <div v-if="schedulesInCell(day.date, slot.value as TimeSlot).length === 0" class="cell-empty">
                + 添加
              </div>
            </div>
          </template>
        </div>
        <p class="week-tip">点击空白格子新增排班，点击医生卡片编辑排班</p>
      </div>

      <AdminAiSchedulePanel
        v-else-if="viewMode === 'ai'"
        :department-id="filters.departmentId"
        @published="loadSchedules"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!editing" label="科室">
          <el-select
            v-model="filters.departmentId"
            placeholder="选择科室"
            style="width: 100%"
            @change="loadDoctors"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="医生" prop="doctorId">
          <el-select
            v-model="form.doctorId"
            placeholder="请选择医生"
            style="width: 100%"
            :disabled="!canEditDoctorFields"
          >
            <el-option
              v-for="doc in doctors"
              :key="doc.id"
              :label="`${doc.realName}（${doc.title}）`"
              :value="doc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker
            v-model="form.workDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled="!canEditDoctorFields"
          />
        </el-form-item>
        <el-form-item label="班次" prop="timeSlot">
          <el-select
            v-model="form.timeSlot"
            style="width: 100%"
            :disabled="!canEditDoctorFields"
          >
            <el-option
              v-for="item in timeSlotOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="号源上限" prop="maxPatients">
          <el-input-number v-model="form.maxPatients" :min="1" :max="200" style="width: 100%" />
        </el-form-item>
        <el-alert
          v-if="editing && !canEditDoctorFields"
          title="已有患者挂号，仅可调整号源上限"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 8px 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.week-range {
  margin: 0 8px;
  font-size: 13px;
  color: #606266;
}

.week-grid-wrap {
  overflow-x: auto;
}

.week-grid {
  display: grid;
  grid-template-columns: 72px repeat(7, minmax(120px, 1fr));
  border: 1px solid #ebeef5;
  min-width: 960px;
}

.week-corner {
  background: #f5f7fa;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.week-day-head {
  padding: 10px 8px;
  text-align: center;
  background: #f5f7fa;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 13px;
}

.day-date {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
  margin-top: 2px;
}

.slot-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.week-cell {
  min-height: 88px;
  padding: 6px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  cursor: pointer;
  transition: background 0.15s;
}

.week-cell:hover {
  background: #f5f9ff;
}

.schedule-chip {
  padding: 6px 8px;
  margin-bottom: 6px;
  border-radius: 6px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.schedule-chip.full {
  background: #fef0f0;
  border-color: #fde2e2;
}

.chip-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.chip-sub {
  font-size: 11px;
  color: #909399;
  margin: 2px 0 4px;
}

.cell-empty {
  height: 100%;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}

.week-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.filters {
  margin-bottom: 16px;
}
</style>
