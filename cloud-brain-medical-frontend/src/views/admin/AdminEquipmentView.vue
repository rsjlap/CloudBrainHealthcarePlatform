<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { EChartsCoreOption } from 'echarts'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import EChart from '@/components/EChart.vue'
import AdminPagination from '@/components/AdminPagination.vue'
import { listDepartmentsApi } from '@/api/department'
import {
  createEquipmentUsageApi,
  createMedicalEquipmentApi,
  deleteMedicalEquipmentApi,
  equipmentCategoryNamesApi,
  equipmentStatsApi,
  equipmentUsageRecordsApi,
  medicalEquipmentListApi,
  updateEquipmentStatusApi,
  updateMedicalEquipmentApi,
  type EquipmentStats,
  type EquipmentStatus,
  type EquipmentUsageRecord,
  type MedicalEquipment,
} from '@/api/adminEquipment'
import type { Department } from '@/types/api'

const activeTab = ref('equipment')
const loading = ref(false)
const saving = ref(false)
const stats = ref<EquipmentStats | null>(null)
const categoryNames = ref<string[]>([])
const departments = ref<Department[]>([])
const equipmentList = ref<MedicalEquipment[]>([])
const equipmentPagination = reactive({ page: 1, size: 10, total: 0 })
const usageRecords = ref<EquipmentUsageRecord[]>([])

const equipmentDialogVisible = ref(false)
const usageDialogVisible = ref(false)
const editingEquipment = ref<MedicalEquipment | null>(null)
const currentEquipment = ref<MedicalEquipment | null>(null)

const equipmentFormRef = ref<FormInstance>()
const usageFormRef = ref<FormInstance>()

const filters = reactive({
  category: '',
  status: '' as EquipmentStatus | '',
  keyword: '',
})

const equipmentForm = reactive({
  equipmentCode: '',
  equipmentName: '',
  category: '',
  departmentId: undefined as number | undefined,
  model: '',
  manufacturer: '',
  location: '',
  purchaseDate: '',
  lastMaintenanceDate: '',
  remark: '',
})

const usageForm = reactive({
  usageType: '检查',
  operatorName: '',
  patientInfo: '',
  startTime: '',
  endTime: '',
  remark: '',
})

const statusOptions = [
  { value: 'ACTIVE', label: '运行中' },
  { value: 'DISABLED', label: '已停用' },
  { value: 'MAINTENANCE', label: '维护中' },
  { value: 'FAULT', label: '故障' },
]

const statusLabel: Record<EquipmentStatus, string> = {
  ACTIVE: '运行中',
  DISABLED: '已停用',
  MAINTENANCE: '维护中',
  FAULT: '故障',
}

const statusType: Record<EquipmentStatus, 'success' | 'info' | 'warning' | 'danger'> = {
  ACTIVE: 'success',
  DISABLED: 'info',
  MAINTENANCE: 'warning',
  FAULT: 'danger',
}

const usageTypeOptions = ['检查', '治疗', '维护巡检', '其他']

const statCards = computed(() => [
  { label: '设备总数', value: stats.value?.totalCount ?? 0, color: '#409eff' },
  { label: '运行中', value: stats.value?.activeCount ?? 0, color: '#67c23a' },
  { label: '维护中', value: stats.value?.maintenanceCount ?? 0, color: '#e6a23c' },
  { label: '故障', value: stats.value?.faultCount ?? 0, color: '#f56c6c' },
  { label: '异常合计', value: stats.value?.abnormalCount ?? 0, color: '#909399' },
])

const statusChartColors: Record<string, string> = {
  运行中: '#67c23a',
  已停用: '#909399',
  维护中: '#e6a23c',
  故障: '#f56c6c',
}

const equipmentStatusPieOption = computed<EChartsCoreOption>(() => {
  const dist = stats.value?.statusDistribution ?? []
  const pieData = dist
    .filter((item) => item.count > 0)
    .map((item) => ({
      name: item.label,
      value: item.count,
      itemStyle: { color: statusChartColors[item.label] ?? '#409eff' },
    }))

  const chartData = pieData.length
    ? pieData
    : [{ name: '暂无设备', value: 1, itemStyle: { color: '#e4e7ed' } }]

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}台 ({d}%)',
      confine: false,
      appendTo: () => document.body,
      extraCssText: 'z-index: 9999; pointer-events: none;',
    },
    legend: {
      bottom: 8,
      left: 'center',
      itemGap: 20,
      formatter: (name: string) => {
        const item = dist.find((d) => d.label === name)
        return item ? `${name}  ${item.count}台` : name
      },
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['42%', '62%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: false,
        stillShowZeroSum: false,
        data: chartData,
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 6,
        },
      },
    ],
  }
})

const equipmentRules: FormRules = {
  equipmentCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  equipmentName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入设备分类', trigger: 'blur' }],
}

const usageRules: FormRules = {
  usageType: [{ required: true, message: '请选择使用类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
}

async function loadCategoryNames() {
  categoryNames.value = await equipmentCategoryNamesApi()
}

async function loadAll() {
  loading.value = true
  try {
    const [statsData, deptData] = await Promise.all([
      equipmentStatsApi(),
      listDepartmentsApi(),
      loadCategoryNames(),
    ])
    stats.value = statsData
    departments.value = deptData
    await loadEquipment()
  } finally {
    loading.value = false
  }
}

async function loadEquipment() {
  const result = await medicalEquipmentListApi({
    category: filters.category || undefined,
    status: filters.status || undefined,
    keyword: filters.keyword || undefined,
    page: equipmentPagination.page,
    size: equipmentPagination.size,
  })
  equipmentList.value = result.records
  equipmentPagination.total = result.total
}

function resetEquipmentForm() {
  equipmentForm.equipmentCode = ''
  equipmentForm.equipmentName = ''
  equipmentForm.category = categoryNames.value[0] || ''
  equipmentForm.departmentId = undefined
  equipmentForm.model = ''
  equipmentForm.manufacturer = ''
  equipmentForm.location = ''
  equipmentForm.purchaseDate = ''
  equipmentForm.lastMaintenanceDate = ''
  equipmentForm.remark = ''
}

function openCreateEquipment() {
  editingEquipment.value = null
  resetEquipmentForm()
  equipmentDialogVisible.value = true
}

function openEditEquipment(row: MedicalEquipment) {
  editingEquipment.value = row
  equipmentForm.equipmentCode = row.equipmentCode
  equipmentForm.equipmentName = row.equipmentName
  equipmentForm.category = row.category
  equipmentForm.departmentId = row.departmentId
  equipmentForm.model = row.model || ''
  equipmentForm.manufacturer = row.manufacturer || ''
  equipmentForm.location = row.location || ''
  equipmentForm.purchaseDate = row.purchaseDate || ''
  equipmentForm.lastMaintenanceDate = row.lastMaintenanceDate || ''
  equipmentForm.remark = row.remark || ''
  equipmentDialogVisible.value = true
}

async function submitEquipment() {
  await equipmentFormRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...equipmentForm, category: equipmentForm.category.trim() }
    if (editingEquipment.value) {
      await updateMedicalEquipmentApi(editingEquipment.value.id, payload)
      ElMessage.success('设备已更新')
    } else {
      await createMedicalEquipmentApi(payload)
      ElMessage.success('设备已新增')
    }
    equipmentDialogVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: MedicalEquipment, status: EquipmentStatus) {
  await updateEquipmentStatusApi(row.id, status)
  ElMessage.success(`已设为${statusLabel[status]}`)
  await loadAll()
}

async function deleteEquipment(row: MedicalEquipment) {
  await ElMessageBox.confirm(`确定删除设备「${row.equipmentName}」吗？`, '删除设备', { type: 'warning' })
  await deleteMedicalEquipmentApi(row.id)
  ElMessage.success('设备已删除')
  await loadAll()
}

async function openUsageDialog(row: MedicalEquipment) {
  currentEquipment.value = row
  usageForm.usageType = '检查'
  usageForm.operatorName = ''
  usageForm.patientInfo = ''
  usageForm.startTime = new Date().toISOString().slice(0, 19)
  usageForm.endTime = ''
  usageForm.remark = ''
  usageRecords.value = await equipmentUsageRecordsApi(row.id)
  usageDialogVisible.value = true
}

async function submitUsage() {
  if (!currentEquipment.value) return
  await usageFormRef.value?.validate()
  saving.value = true
  try {
    await createEquipmentUsageApi(currentEquipment.value.id, {
      usageType: usageForm.usageType,
      operatorName: usageForm.operatorName,
      patientInfo: usageForm.patientInfo,
      startTime: usageForm.startTime,
      endTime: usageForm.endTime || undefined,
      remark: usageForm.remark,
    })
    ElMessage.success('使用记录已登记')
    usageRecords.value = await equipmentUsageRecordsApi(currentEquipment.value.id)
    usageForm.operatorName = ''
    usageForm.patientInfo = ''
    usageForm.remark = ''
  } finally {
    saving.value = false
  }
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadAll)
</script>

<template>
  <div class="page" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in statCards" :key="card.label" :span="4" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="设备列表" name="equipment">
          <div class="toolbar">
            <el-form :inline="true">
              <el-form-item label="分类">
                <el-select
                  v-model="filters.category"
                  clearable
                  filterable
                  allow-create
                  default-first-option
                  placeholder="全部"
                  style="width: 140px"
                  @change="loadEquipment"
                >
                  <el-option v-for="name in categoryNames" :key="name" :label="name" :value="name" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px" @change="loadEquipment">
                  <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="关键词">
                <el-input v-model="filters.keyword" placeholder="编号/名称" clearable @keyup.enter="loadEquipment" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="loadEquipment">查询</el-button>
                <el-button type="success" @click="openCreateEquipment">新增设备</el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table :data="equipmentList" stripe>
            <el-table-column prop="equipmentCode" label="编号" width="120" />
            <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="departmentName" label="科室" width="100" />
            <el-table-column prop="location" label="位置" width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }: { row: MedicalEquipment }">
                <el-tag :type="statusType[row.status]" size="small">{{ statusLabel[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="manufacturer" label="厂商" width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }: { row: MedicalEquipment }">
                <el-button type="primary" link @click="openEditEquipment(row)">编辑</el-button>
                <el-button type="primary" link @click="openUsageDialog(row)">使用记录</el-button>
                <el-dropdown trigger="click" @command="(cmd: EquipmentStatus) => changeStatus(row, cmd)">
                  <el-button type="warning" link>状态</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="ACTIVE">启用</el-dropdown-item>
                      <el-dropdown-item command="DISABLED">停用</el-dropdown-item>
                      <el-dropdown-item command="MAINTENANCE">维护</el-dropdown-item>
                      <el-dropdown-item command="FAULT">标记故障</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="danger" link @click="deleteEquipment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <AdminPagination
            v-model:page="equipmentPagination.page"
            v-model:size="equipmentPagination.size"
            :total="equipmentPagination.total"
            @change="loadEquipment"
          />
        </el-tab-pane>

        <el-tab-pane label="状态统计" name="stats" lazy>
          <el-row :gutter="24" class="stats-panel">
            <el-col :span="12" :xs="24">
              <div class="stats-chart-panel">
                <EChart v-if="stats && activeTab === 'stats'" :option="equipmentStatusPieOption" />
              </div>
            </el-col>
            <el-col :span="12" :xs="24">
              <div class="stats-table-panel">
                <el-table :data="stats?.statusDistribution || []" stripe size="small" style="width: 100%">
                  <el-table-column prop="label" label="状态" />
                  <el-table-column prop="count" label="数量" width="80" align="center" />
                  <el-table-column label="占比" width="90" align="center">
                    <template #default="{ row }">
                      {{ stats?.totalCount ? ((row.count / stats.totalCount) * 100).toFixed(1) : 0 }}%
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
          </el-row>
          <el-alert
            class="stats-tip"
            title="异常状态包括：已停用、维护中、故障。可在设备列表中通过「状态」操作进行启用、停用或维护。"
            type="warning"
            :closable="false"
            show-icon
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="equipmentDialogVisible" :title="editingEquipment ? '编辑设备' : '新增设备'" width="560px" destroy-on-close>
      <el-form ref="equipmentFormRef" :model="equipmentForm" :rules="equipmentRules" label-width="100px">
        <el-form-item label="设备编号" prop="equipmentCode">
          <el-input v-model="equipmentForm.equipmentCode" />
        </el-form-item>
        <el-form-item label="设备名称" prop="equipmentName">
          <el-input v-model="equipmentForm.equipmentName" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select
            v-model="equipmentForm.category"
            filterable
            allow-create
            default-first-option
            placeholder="输入或选择分类"
            style="width: 100%"
          >
            <el-option v-for="name in categoryNames" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属科室">
          <el-select v-model="equipmentForm.departmentId" clearable style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号"><el-input v-model="equipmentForm.model" /></el-form-item>
        <el-form-item label="厂商"><el-input v-model="equipmentForm.manufacturer" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="equipmentForm.location" /></el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="equipmentForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最近维护">
          <el-date-picker v-model="equipmentForm.lastMaintenanceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="equipmentForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="equipmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEquipment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="usageDialogVisible" :title="`使用记录 - ${currentEquipment?.equipmentName || ''}`" width="720px" destroy-on-close>
      <el-form ref="usageFormRef" :model="usageForm" :rules="usageRules" :inline="true" class="usage-form">
        <el-form-item label="类型" prop="usageType">
          <el-select v-model="usageForm.usageType" style="width: 120px">
            <el-option v-for="t in usageTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人"><el-input v-model="usageForm.operatorName" style="width: 100px" /></el-form-item>
        <el-form-item label="患者"><el-input v-model="usageForm.patientInfo" style="width: 120px" /></el-form-item>
        <el-form-item label="开始" prop="startTime">
          <el-date-picker v-model="usageForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" :disabled="currentEquipment?.status !== 'ACTIVE'" @click="submitUsage">
            登记使用
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="currentEquipment?.status !== 'ACTIVE'" title="仅运行中的设备可登记使用" type="warning" :closable="false" show-icon class="usage-tip" />
      <el-table :data="usageRecords" stripe max-height="320">
        <el-table-column prop="usageType" label="类型" width="90" />
        <el-table-column prop="operatorName" label="操作人" width="90" />
        <el-table-column prop="patientInfo" label="患者" width="100" />
        <el-table-column label="开始时间" width="150">
          <template #default="{ row }: { row: EquipmentUsageRecord }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="150">
          <template #default="{ row }: { row: EquipmentUsageRecord }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { padding: 8px 0; }
.stat-row { margin-bottom: 16px; }
.stat-card { margin-bottom: 12px; text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { margin-top: 6px; color: #909399; font-size: 13px; }
.toolbar { margin-bottom: 16px; }
.stats-panel { align-items: stretch; }
.stats-chart-panel {
  width: 100%;
  min-height: 360px;
  padding: 8px 12px 0;
  overflow: visible;
}
.stats-chart-panel :deep(.echart-box) {
  width: 100%;
  max-width: 420px;
  height: 340px;
  margin: 0 auto;
}
.stats-table-panel {
  display: flex;
  align-items: center;
  min-height: 340px;
  padding: 8px 0;
}
.stats-tip, .usage-tip { margin-top: 16px; }
.usage-form { margin-bottom: 12px; }
</style>
