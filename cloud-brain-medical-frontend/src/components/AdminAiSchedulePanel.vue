<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { generateAiScheduleApi, type AiScheduleSuggestion, type AiTimeSlot } from '@/api/ai'
import { batchPublishAdminSchedulesApi } from '@/api/adminSchedule'
import { listDoctorsApi } from '@/api/doctor'
import type { DoctorCard } from '@/types/api'

interface DraftSchedule extends AiScheduleSuggestion {
  clientId: string
}

const props = defineProps<{
  departmentId?: number
}>()

const emit = defineEmits<{
  published: []
}>()

const loading = ref(false)
const generating = ref(false)
const publishing = ref(false)
const doctors = ref<DoctorCard[]>([])
const drafts = ref<DraftSchedule[]>([])
const dragging = ref<DraftSchedule | null>(null)
const aiSummary = ref('')

const form = reactive({
  dateRange: [] as string[],
})

const timeSlotOptions = [
  { value: 'MORNING' as AiTimeSlot, label: '上午' },
  { value: 'AFTERNOON' as AiTimeSlot, label: '下午' },
  { value: 'EVENING' as AiTimeSlot, label: '晚上' },
]

const timeSlotLabel: Record<AiTimeSlot, string> = {
  MORNING: '上午',
  AFTERNOON: '下午',
  EVENING: '晚上',
}

const gridDays = computed(() => {
  if (form.dateRange.length !== 2) return []
  const [start, end] = form.dateRange
  const days: Array<{ date: string; label: string; short: string }> = []
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  let current = start!
  while (current <= end!) {
    const d = new Date(current)
    days.push({
      date: current,
      label: labels[d.getDay()]!,
      short: current.slice(5),
    })
    const next = new Date(current)
    next.setDate(next.getDate() + 1)
    current = next.toISOString().slice(0, 10)
  }
  return days
})

function newClientId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function itemsInCell(date: string, slot: AiTimeSlot) {
  return drafts.value.filter((d) => d.workDate === date && d.timeSlot === slot)
}

async function loadDoctors() {
  if (!props.departmentId) {
    doctors.value = []
    return
  }
  doctors.value = await listDoctorsApi(props.departmentId)
}

async function handleGenerate() {
  if (!props.departmentId) {
    ElMessage.warning('请先选择科室')
    return
  }
  if (form.dateRange.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  generating.value = true
  aiSummary.value = ''
  try {
    const result = await generateAiScheduleApi({
      deptId: props.departmentId,
      startDate: form.dateRange[0]!,
      endDate: form.dateRange[1]!,
    })
    drafts.value = result.schedules.map((item) => ({ ...item, clientId: newClientId() }))
    aiSummary.value = result.summary || ''
    if (drafts.value.length > 0) {
      await loadDoctors()
    }
    if (drafts.value.length === 0) {
      ElMessage.info('未生成有效排班建议，请调整时间范围后重试')
    } else {
      ElMessage.success(`DeepSeek 已生成 ${drafts.value.length} 条排班建议，可拖拽调整`)
    }
  } finally {
    generating.value = false
  }
}

function onDragStart(item: DraftSchedule) {
  dragging.value = item
}

function onDrop(date: string, slot: AiTimeSlot) {
  if (!dragging.value) return
  const target = dragging.value
  const conflict = drafts.value.some(
    (d) =>
      d.clientId !== target.clientId &&
      d.doctorId === target.doctorId &&
      d.workDate === date &&
      d.timeSlot === slot,
  )
  if (conflict) {
    ElMessage.warning('该医生在此班次已有建议，请先移走或删除')
    dragging.value = null
    return
  }
  target.workDate = date
  target.timeSlot = slot
  dragging.value = null
}

function onDoctorChange(row: DraftSchedule, doctorId: number) {
  const doctor = doctors.value.find((d) => d.id === doctorId)
  if (!doctor) return
  const conflict = drafts.value.some(
    (d) =>
      d.clientId !== row.clientId &&
      d.doctorId === doctorId &&
      d.workDate === row.workDate &&
      d.timeSlot === row.timeSlot,
  )
  if (conflict) {
    ElMessage.warning('该医生在此班次已有建议')
    return
  }
  row.doctorId = doctorId
  row.doctorName = doctor.realName
}

function removeDraft(row: DraftSchedule) {
  drafts.value = drafts.value.filter((d) => d.clientId !== row.clientId)
}

async function addDraftRow() {
  if (form.dateRange.length !== 2) {
    ElMessage.warning('请先选择科室和时间范围')
    return
  }
  await loadDoctors()
  if (!doctors.value.length) {
    ElMessage.warning('该科室暂无可用医生')
    return
  }
  const doctor = doctors.value[0]!
  drafts.value.push({
    clientId: newClientId(),
    doctorId: doctor.id,
    doctorName: doctor.realName,
    workDate: form.dateRange[0]!,
    timeSlot: 'MORNING',
    maxPatients: 20,
    reason: '手动添加',
  })
}

async function handlePublish() {
  if (drafts.value.length === 0) {
    ElMessage.warning('暂无待发布的排班建议')
    return
  }
  await ElMessageBox.confirm(
    `确认发布 ${drafts.value.length} 条排班到系统？已存在的排班将自动跳过。`,
    '发布确认',
    { type: 'warning' },
  )
  publishing.value = true
  try {
    const result = await batchPublishAdminSchedulesApi(
      drafts.value.map((d) => ({
        doctorId: d.doctorId,
        workDate: d.workDate,
        timeSlot: d.timeSlot,
        maxPatients: d.maxPatients,
      })),
    )
    ElMessage.success(
      `发布完成：成功 ${result.successCount} 条，跳过 ${result.skippedCount} 条`,
    )
    if (result.errors.length > 0) {
      ElMessage.warning(result.errors.slice(0, 3).join('；'))
    }
    drafts.value = []
    aiSummary.value = ''
    emit('published')
  } finally {
    publishing.value = false
  }
}

watch(
  () => props.departmentId,
  () => {
    doctors.value = []
    drafts.value = []
    aiSummary.value = ''
  },
)
</script>

<template>
  <div v-loading="loading" class="ai-panel">
    <el-alert
      title="选择科室与时间范围后，将调用 DeepSeek 生成排班建议。可在表格中编辑，或在周历格子间拖拽调整，确认后一键发布。"
      type="info"
      :closable="false"
      show-icon
      class="tip"
    />

    <el-form inline class="filters">
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="handleGenerate">
          DeepSeek 生成排班
        </el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="aiSummary"
      :title="aiSummary"
      type="success"
      :closable="false"
      show-icon
      class="summary"
    />

    <template v-if="drafts.length > 0">
      <div class="result-header">
        <span>排班建议（{{ drafts.length }} 条）</span>
        <div class="header-actions">
          <el-button @click="addDraftRow">手动添加</el-button>
          <el-button type="success" :loading="publishing" @click="handlePublish">发布确认</el-button>
        </div>
      </div>

      <el-table :data="drafts" stripe border size="small" class="draft-table">
        <el-table-column prop="workDate" label="日期" width="120" />
        <el-table-column label="班次" width="100">
          <template #default="{ row }: { row: DraftSchedule }">{{ timeSlotLabel[row.timeSlot] }}</template>
        </el-table-column>
        <el-table-column label="医生" width="160">
          <template #default="{ row }">
            <el-select
              :model-value="row.doctorId"
              size="small"
              style="width: 140px"
              @change="(id: number) => onDoctorChange(row, id)"
            >
              <el-option v-for="doc in doctors" :key="doc.id" :label="doc.realName" :value="doc.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="号源上限" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.maxPatients" :min="1" :max="100" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="AI 说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeDraft(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="gridDays.length > 0" class="grid-section">
        <p class="grid-tip">拖拽下方卡片到其他日期/班次格子可调整排班（表格会同步更新）</p>
        <div class="week-grid-wrap">
          <div class="week-grid" :style="{ gridTemplateColumns: `56px repeat(${gridDays.length}, minmax(88px, 1fr))` }">
            <div class="week-corner" />
            <div v-for="day in gridDays" :key="day.date" class="week-day-head">
              <div>{{ day.label }}</div>
              <div class="day-date">{{ day.short }}</div>
            </div>
            <template v-for="slot in timeSlotOptions" :key="slot.value">
              <div class="slot-label">{{ slot.label }}</div>
              <div
                v-for="day in gridDays"
                :key="`${slot.value}-${day.date}`"
                class="week-cell"
                @dragover.prevent
                @drop="onDrop(day.date, slot.value)"
              >
                <div
                  v-for="item in itemsInCell(day.date, slot.value)"
                  :key="item.clientId"
                  class="schedule-chip"
                  draggable="true"
                  @dragstart="onDragStart(item)"
                >
                  <div class="chip-name">{{ item.doctorName }}</div>
                  <div class="chip-sub">{{ item.maxPatients }} 号源</div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!generating" description="请选择科室和时间范围，点击「DeepSeek 生成排班」" />
  </div>
</template>

<style scoped>
.ai-panel {
  padding-top: 4px;
}

.tip {
  margin-bottom: 16px;
}

.summary {
  margin-bottom: 16px;
}

.filters {
  margin-bottom: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.draft-table {
  margin-bottom: 20px;
}

.grid-section {
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.grid-tip {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
}

.week-grid-wrap {
  overflow-x: auto;
}

.week-grid {
  display: grid;
  min-width: 600px;
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
  padding: 8px 4px;
  text-align: center;
  background: #f5f7fa;
  font-size: 12px;
}

.day-date {
  color: #909399;
  font-size: 11px;
}

.slot-label {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  background: #fafafa;
  font-size: 12px;
  color: #606266;
}

.week-cell {
  min-height: 72px;
  padding: 4px;
  background: #fff;
}

.schedule-chip {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  padding: 4px 6px;
  margin-bottom: 4px;
  cursor: grab;
  user-select: none;
}

.schedule-chip:active {
  cursor: grabbing;
  opacity: 0.85;
}

.chip-name {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
}

.chip-sub {
  font-size: 11px;
  color: #909399;
}
</style>
