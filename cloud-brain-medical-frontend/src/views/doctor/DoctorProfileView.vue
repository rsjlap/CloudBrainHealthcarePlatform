<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  doctorSchedulesApi,
  getDoctorProfileApi,
  updateDoctorProfileApi,
} from '@/api/doctor'
import type { DoctorProfile, Schedule } from '@/types/api'

type CalendarMode = 'week' | 'month'
type TimeSlot = Schedule['timeSlot']

const activeTab = ref('profile')
const profileLoading = ref(false)
const saving = ref(false)
const scheduleLoading = ref(false)
const profile = ref<DoctorProfile | null>(null)
const schedules = ref<Schedule[]>([])
const editing = ref(false)
const formRef = ref<FormInstance>()

const calendarMode = ref<CalendarMode>('week')
const weekStart = ref(getMonday())
const monthCursor = ref(new Date().toISOString().slice(0, 7))

const form = reactive({
  specialty: '',
  introduction: '',
})

const rules: FormRules = {
  specialty: [{ max: 100, message: '专长不超过 100 字', trigger: 'blur' }],
  introduction: [{ max: 500, message: '简介不超过 500 字', trigger: 'blur' }],
}

const timeSlotOptions = [
  { value: 'MORNING' as TimeSlot, label: '上午' },
  { value: 'AFTERNOON' as TimeSlot, label: '下午' },
  { value: 'EVENING' as TimeSlot, label: '晚上' },
]

const timeSlotLabel: Record<TimeSlot, string> = {
  MORNING: '上午',
  AFTERNOON: '下午',
  EVENING: '晚上',
}

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

const weekDays = computed(() => {
  const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return labels.map((label, index) => {
    const date = addDays(weekStart.value, index)
    return { label, date, short: date.slice(5) }
  })
})

const weekEnd = computed(() => addDays(weekStart.value, 6))

const weekTitle = computed(() => `${weekStart.value} ~ ${weekEnd.value}`)

const monthTitle = computed(() => {
  const [y, m] = monthCursor.value.split('-')
  return `${y}年${Number(m)}月`
})

const monthDays = computed(() => {
  const [yearStr, monthStr] = monthCursor.value.split('-')
  const year = Number(yearStr)
  const month = Number(monthStr)
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const startOffset = (firstDay.getDay() + 6) % 7
  const cells: Array<{ date: string; day: number; inMonth: boolean } | null> = []

  for (let i = 0; i < startOffset; i += 1) {
    cells.push(null)
  }
  for (let day = 1; day <= lastDay.getDate(); day += 1) {
    const date = `${yearStr}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    cells.push({ date, day, inMonth: true })
  }
  while (cells.length % 7 !== 0) {
    cells.push(null)
  }
  return cells
})

const rangeStart = computed(() =>
  calendarMode.value === 'week' ? weekStart.value : `${monthCursor.value}-01`,
)

const rangeEnd = computed(() => {
  if (calendarMode.value === 'week') {
    return weekEnd.value
  }
  const [yearStr, monthStr] = monthCursor.value.split('-')
  const year = Number(yearStr)
  const month = Number(monthStr)
  const lastDay = new Date(year, month, 0).getDate()
  return `${monthCursor.value}-${String(lastDay).padStart(2, '0')}`
})

function schedulesInCell(date: string, slot: TimeSlot) {
  return schedules.value.filter((s) => s.workDate === date && s.timeSlot === slot)
}

function schedulesOnDate(date: string) {
  return schedules.value.filter((s) => s.workDate === date)
}

function slotProgress(item: Schedule) {
  const pct = item.maxPatients ? Math.round((item.registeredCount / item.maxPatients) * 100) : 0
  return Math.min(pct, 100)
}

function prevPeriod() {
  if (calendarMode.value === 'week') {
    weekStart.value = addDays(weekStart.value, -7)
  } else {
    const [y, m] = monthCursor.value.split('-').map(Number)
    const d = new Date(y!, m! - 2, 1)
    monthCursor.value = d.toISOString().slice(0, 7)
  }
}

function nextPeriod() {
  if (calendarMode.value === 'week') {
    weekStart.value = addDays(weekStart.value, 7)
  } else {
    const [y, m] = monthCursor.value.split('-').map(Number)
    const d = new Date(y!, m!, 1)
    monthCursor.value = d.toISOString().slice(0, 7)
  }
}

function goCurrentPeriod() {
  if (calendarMode.value === 'week') {
    weekStart.value = getMonday()
  } else {
    monthCursor.value = new Date().toISOString().slice(0, 7)
  }
}

async function loadProfile() {
  profileLoading.value = true
  try {
    profile.value = await getDoctorProfileApi()
    form.specialty = profile.value.specialty || ''
    form.introduction = profile.value.introduction || ''
  } catch {
    ElMessage.error('加载个人资料失败')
  } finally {
    profileLoading.value = false
  }
}

function startEdit() {
  if (!profile.value) return
  form.specialty = profile.value.specialty || ''
  form.introduction = profile.value.introduction || ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  if (profile.value) {
    form.specialty = profile.value.specialty || ''
    form.introduction = profile.value.introduction || ''
  }
}

async function saveProfile() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    profile.value = await updateDoctorProfileApi({
      specialty: form.specialty.trim(),
      introduction: form.introduction.trim(),
    })
    editing.value = false
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function loadSchedules() {
  scheduleLoading.value = true
  try {
    schedules.value = await doctorSchedulesApi(rangeStart.value, rangeEnd.value)
  } catch {
    ElMessage.error('加载排班失败')
  } finally {
    scheduleLoading.value = false
  }
}

watch([activeTab, calendarMode, weekStart, monthCursor], () => {
  if (activeTab.value === 'schedule') {
    loadSchedules()
  }
})

onMounted(async () => {
  await loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
      <p class="page-desc">管理个人信息与查看排班安排</p>
    </div>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="个人信息" name="profile">
        <el-card v-loading="profileLoading" shadow="never">
          <template v-if="profile && !editing">
            <div class="card-toolbar">
              <el-button type="primary" @click="startEdit">编辑专长简介</el-button>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ profile.realName }}</el-descriptions-item>
              <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
              <el-descriptions-item label="职称">{{ profile.title || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属科室">{{ profile.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="挂号费">
                {{ profile.registrationFee != null ? `¥${profile.registrationFee}` : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="专长" :span="2">
                {{ profile.specialty || '暂未填写' }}
              </el-descriptions-item>
              <el-descriptions-item label="个人简介" :span="2">
                {{ profile.introduction || '暂未填写' }}
              </el-descriptions-item>
            </el-descriptions>
          </template>

          <template v-else-if="profile && editing">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
              <el-form-item label="姓名">
                <span>{{ profile.realName }}</span>
              </el-form-item>
              <el-form-item label="科室">
                <span>{{ profile.departmentName || '-' }}</span>
              </el-form-item>
              <el-form-item label="专长" prop="specialty">
                <el-input
                  v-model="form.specialty"
                  placeholder="如：心血管疾病、高血压"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="个人简介" prop="introduction">
                <el-input
                  v-model="form.introduction"
                  type="textarea"
                  :rows="5"
                  placeholder="介绍您的从业经历、擅长领域等"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
                <el-button @click="cancelEdit">取消</el-button>
              </el-form-item>
            </el-form>
          </template>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的排班" name="schedule">
        <el-card shadow="never">
          <div class="schedule-toolbar">
            <el-radio-group v-model="calendarMode" size="small">
              <el-radio-button value="week">周视图</el-radio-button>
              <el-radio-button value="month">月视图</el-radio-button>
            </el-radio-group>
            <div class="period-nav">
              <el-button :icon="ArrowLeft" size="small" @click="prevPeriod" />
              <span class="period-title">
                {{ calendarMode === 'week' ? weekTitle : monthTitle }}
              </span>
              <el-button :icon="ArrowRight" size="small" @click="nextPeriod" />
              <el-button size="small" @click="goCurrentPeriod">今天</el-button>
            </div>
          </div>

          <div v-if="calendarMode === 'week'" v-loading="scheduleLoading" class="week-grid-wrap">
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
                >
                  <div
                    v-for="item in schedulesInCell(day.date, slot.value)"
                    :key="item.id"
                    class="schedule-chip"
                    :class="{ full: item.remainingSlots <= 0 }"
                  >
                    <div class="chip-title">{{ timeSlotLabel[item.timeSlot] }}</div>
                    <div class="chip-sub">{{ item.registeredCount }}/{{ item.maxPatients }} 已挂</div>
                    <el-progress
                      :percentage="slotProgress(item)"
                      :stroke-width="4"
                      :show-text="false"
                      :status="item.remainingSlots <= 0 ? 'exception' : undefined"
                    />
                  </div>
                  <div
                    v-if="schedulesInCell(day.date, slot.value).length === 0"
                    class="cell-empty"
                  >
                    无排班
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div v-else v-loading="scheduleLoading" class="month-grid-wrap">
            <div class="month-week-head">
              <div v-for="label in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="label">
                {{ label }}
              </div>
            </div>
            <div class="month-grid">
              <div
                v-for="(cell, idx) in monthDays"
                :key="idx"
                class="month-cell"
                :class="{ empty: !cell }"
              >
                <template v-if="cell">
                  <div class="month-day-num">{{ cell.day }}</div>
                  <div
                    v-for="item in schedulesOnDate(cell.date)"
                    :key="item.id"
                    class="month-chip"
                    :class="{ full: item.remainingSlots <= 0 }"
                  >
                    {{ timeSlotLabel[item.timeSlot] }} {{ item.registeredCount }}/{{ item.maxPatients }}
                  </div>
                </template>
              </div>
            </div>
          </div>

          <el-empty v-if="!scheduleLoading && schedules.length === 0" description="该时段暂无排班" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile-page {
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

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.card-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.schedule-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.period-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.period-title {
  min-width: 180px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.week-grid-wrap {
  overflow-x: auto;
}

.week-grid {
  display: grid;
  grid-template-columns: 56px repeat(7, minmax(100px, 1fr));
  min-width: 820px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.week-corner,
.week-day-head,
.slot-label,
.week-cell {
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.week-corner {
  background: #f5f7fa;
}

.week-day-head {
  padding: 8px;
  text-align: center;
  background: #f5f7fa;
  font-size: 13px;
}

.day-date {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.slot-label {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  background: #fafafa;
  font-size: 13px;
  color: #606266;
}

.week-cell {
  min-height: 88px;
  padding: 6px;
  background: #fff;
}

.schedule-chip {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  padding: 6px 8px;
  margin-bottom: 4px;
}

.schedule-chip.full {
  background: #fef0f0;
  border-color: #fde2e2;
}

.chip-title {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
}

.schedule-chip.full .chip-title {
  color: #f56c6c;
}

.chip-sub {
  font-size: 11px;
  color: #909399;
  margin: 2px 0 4px;
}

.cell-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}

.month-grid-wrap {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.month-week-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.month-week-head > div {
  padding: 8px;
  text-align: center;
  font-size: 13px;
  color: #606266;
  border-right: 1px solid #ebeef5;
}

.month-week-head > div:last-child {
  border-right: none;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.month-cell {
  min-height: 96px;
  padding: 6px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.month-cell.empty {
  background: #fafafa;
}

.month-day-num {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.month-chip {
  font-size: 11px;
  padding: 2px 6px;
  margin-bottom: 2px;
  border-radius: 4px;
  background: #ecf5ff;
  color: #409eff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.month-chip.full {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
