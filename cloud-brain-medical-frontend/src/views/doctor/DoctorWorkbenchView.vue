<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, DArrowLeft, DArrowRight, Refresh } from '@element-plus/icons-vue'
import {
  callRegistrationApi,
  completeRegistrationApi,
  doctorQueueApi,
  registrationPatientInfoApi,
} from '@/api/registration'
import type { Registration, RegistrationPatientInfo } from '@/types/api'
import DoctorEmrForm from './components/DoctorEmrForm.vue'

const route = useRoute()

type ListMode = 'queue' | 'consulting'

const workDate = ref(new Date().toISOString().slice(0, 10))
const listMode = ref<ListMode>('queue')
const queue = ref<Registration[]>([])
const loading = ref(false)
const calling = ref(false)
const selectedId = ref<number>()
const patientInfo = ref<RegistrationPatientInfo | null>(null)
const patientInfoMap = ref<Record<number, RegistrationPatientInfo>>({})
const expandedIds = ref<number[]>([])
const loadingProfileIds = ref<number[]>([])
const sidebarCollapsed = ref(false)

const statusLabel: Record<string, string> = {
  PENDING: '待就诊',
  IN_PROGRESS: '在诊',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusTagType: Record<string, 'warning' | 'success' | 'info' | 'danger'> = {
  PENDING: 'warning',
  IN_PROGRESS: 'success',
  COMPLETED: 'info',
  CANCELLED: 'danger',
}

const genderLabel = (gender?: number) => {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '-'
}

const filteredList = computed(() => {
  const list = queue.value.filter((r) => r.status !== 'CANCELLED' && r.status !== 'COMPLETED')//去掉已取消和已完成的
  if (listMode.value === 'queue') {
    return list.filter((r) => r.status === 'PENDING')
  }
  return list.filter((r) => r.status === 'IN_PROGRESS')
})

const selectedRegistration = computed(() =>//从完整队列中找到当前选中的挂号记录
  queue.value.find((r) => r.id === selectedId.value),
)

const showEmr = computed(
  () => selectedRegistration.value?.status === 'IN_PROGRESS' && !!patientInfo.value,
)

function toggleSidebar() {//左侧栏展开/收起
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function isExpanded(id: number) {//判断某个患者的具体情况卡片是否展开
  return expandedIds.value.includes(id)
}

function isProfileLoading(id: number) {
  return loadingProfileIds.value.includes(id)
}

async function ensurePatientInfo(id: number) {//带缓存的拉取患者详情
  if (patientInfoMap.value[id]) {
    return patientInfoMap.value[id]
  }
  loadingProfileIds.value = [...loadingProfileIds.value, id]
  try {
    const info = await registrationPatientInfoApi(id)
    patientInfoMap.value = { ...patientInfoMap.value, [id]: info }
    return info
  } finally {
    loadingProfileIds.value = loadingProfileIds.value.filter((x) => x !== id)
  }
}

async function loadQueue() {
  loading.value = true
  try {
    queue.value = await doctorQueueApi(workDate.value)
    if (selectedId.value && !queue.value.some((r) => r.id === selectedId.value)) {
      selectedId.value = undefined
      patientInfo.value = null
    } else if (!selectedId.value && filteredList.value.length > 0) {
      const inProgress = filteredList.value.find((r) => r.status === 'IN_PROGRESS')
      await selectPatient(inProgress ?? filteredList.value[0]!)
    }
  } finally {
    loading.value = false
  }
}

async function loadPatientInfo(id?: number) {
  if (!id) {
    patientInfo.value = null
    return
  }
  patientInfo.value = await ensurePatientInfo(id)
}

async function togglePatientProfile(id: number) {
  if (isExpanded(id)) {
    expandedIds.value = expandedIds.value.filter((x) => x !== id)
    return
  }
  expandedIds.value = [...expandedIds.value, id]
  await ensurePatientInfo(id)
}

async function selectPatient(row: Registration) {
  selectedId.value = row.id
  await loadPatientInfo(row.id)
  if (row.status === 'IN_PROGRESS' && !isExpanded(row.id)) {
    expandedIds.value = [...expandedIds.value, row.id]
    await ensurePatientInfo(row.id)
  }
}

async function onCall(row: Registration) {
  if (!row.paid) {
    ElMessage.warning('患者尚未缴纳挂号费')
    return
  }
  calling.value = true
  try {
    await callRegistrationApi(row.id)
    ElMessage.success(`已叫号：${row.patientName}，请填写门诊病历`)
    listMode.value = 'consulting'
    await loadQueue()
    const updated = queue.value.find((r) => r.id === row.id)
    if (updated) {
      await selectPatient(updated)
    }
  } finally {
    calling.value = false
  }
}

async function onCompleteFromEmr() {
  if (!selectedId.value) return
  await completeRegistrationApi(selectedId.value)
  ElMessage.success('看诊已结束')
  expandedIds.value = expandedIds.value.filter((x) => x !== selectedId.value)
  const id = selectedId.value
  selectedId.value = undefined
  patientInfo.value = null
  const { [id]: _, ...rest } = patientInfoMap.value
  patientInfoMap.value = rest
  await loadQueue()
}

function initFromRoute() {
  if (route.query.registrationId) {
    selectedId.value = Number(route.query.registrationId)
  }
  if (route.query.workDate) {
    workDate.value = route.query.workDate as string
  }
}

watch(selectedId, (id) => loadPatientInfo(id))

watch(listMode, async () => {
  if (filteredList.value.length === 0) {
    selectedId.value = undefined
    patientInfo.value = null
    return
  }
  if (!filteredList.value.some((r) => r.id === selectedId.value)) {
    await selectPatient(filteredList.value[0]!)
  }
})

onMounted(async () => {
  initFromRoute()
  await loadQueue()
  if (selectedId.value) {
    await loadPatientInfo(selectedId.value)
  }
})
</script>

<template>
  <div class="workbench">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <button
        type="button"
        class="sidebar-toggle"
        :title="sidebarCollapsed ? '展开患者列表' : '收起患者列表'"
        @click="toggleSidebar"
      >
        <el-icon>
          <DArrowLeft v-if="!sidebarCollapsed" />
          <DArrowRight v-else />
        </el-icon>
      </button>

      <div v-if="sidebarCollapsed" class="sidebar-rail">
        <span class="rail-text">患者队列</span>
        <span class="rail-count">{{ filteredList.length }}</span>
        <span v-if="selectedRegistration" class="rail-current">
          {{ selectedRegistration.queueNo }}号
        </span>
      </div>

      <div v-show="!sidebarCollapsed" class="sidebar-content">
      <div class="sidebar-tabs">
        <button
          class="tab-btn"
          :class="{ active: listMode === 'queue' }"
          @click="listMode = 'queue'"
        >
          患者队列
        </button>
        <button
          class="tab-btn"
          :class="{ active: listMode === 'consulting' }"
          @click="listMode = 'consulting'"
        >
          在诊患者
        </button>
      </div>

      <div class="sidebar-toolbar">
        <el-date-picker
          v-model="workDate"
          type="date"
          value-format="YYYY-MM-DD"
          size="small"
          style="flex: 1"
          @change="loadQueue"
        />
        <el-button :icon="Refresh" size="small" :loading="loading" @click="loadQueue" />
      </div>

      <div v-loading="loading" class="patient-list">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="patient-block"
          :class="{ active: selectedId === item.id }"
        >
          <div class="patient-item" @click="selectPatient(item)">
            <div class="item-top">
              <span class="queue-no">{{ item.queueNo }}号</span>
              <span class="name">{{ item.patientName }}</span>
              <el-tag :type="statusTagType[item.status]" size="small">
                {{ statusLabel[item.status] }}
              </el-tag>
            </div>
            <div class="item-sub">{{ item.regNo }}</div>
            <div v-if="listMode === 'queue' && item.status === 'PENDING'" class="item-actions">
              <el-button
                type="primary"
                size="small"
                :disabled="!item.paid"
                :loading="calling"
                @click.stop="onCall(item)"
              >
                叫号
              </el-button>
            </div>
            <button
              type="button"
              class="profile-toggle"
              @click.stop="togglePatientProfile(item.id)"
            >
              <span>患者情况</span>
              <el-icon>
                <ArrowUp v-if="isExpanded(item.id)" />
                <ArrowDown v-else />
              </el-icon>
            </button>
          </div>

          <div v-show="isExpanded(item.id)" class="patient-profile-inline">
            <div v-if="isProfileLoading(item.id)" class="profile-loading">加载中...</div>
            <template v-else-if="patientInfoMap[item.id]">
              <div class="profile-row">
                <span class="label">性别</span>{{ genderLabel(patientInfoMap[item.id].gender) }}
              </div>
              <div class="profile-row">
                <span class="label">科室</span>{{ item.deptName }}
              </div>
              <div class="profile-row">
                <span class="label">缴费</span>
                <el-tag :type="item.paid ? 'success' : 'warning'" size="small">
                  {{ item.paid ? '已缴' : '待缴' }}
                </el-tag>
              </div>
              <div class="profile-row">
                <span class="label">主诉</span>{{ item.chiefComplaint || '-' }}
              </div>
              <div class="profile-row">
                <span class="label">过敏史</span>
                <span :class="{ danger: patientInfoMap[item.id].allergyHistory }">
                  {{ patientInfoMap[item.id].allergyHistory || '无' }}
                </span>
              </div>
              <div class="profile-row">
                <span class="label">既往史</span>{{ patientInfoMap[item.id].medicalHistory || '无' }}
              </div>
            </template>
          </div>
        </div>
        <el-empty v-if="!loading && filteredList.length === 0" :image-size="64" description="暂无患者" />
      </div>
      </div>
    </aside>

    <section class="workspace">
      <DoctorEmrForm
        v-if="showEmr"
        :key="selectedId"
        :registration-id="selectedId"
        :registration="selectedRegistration"
        :patient-info="patientInfo"
        @complete="onCompleteFromEmr"
      />
      <div v-else class="workspace-placeholder">
        <el-empty :image-size="120">
          <template #description>
            <p v-if="!selectedRegistration">请从左侧选择患者</p>
            <p v-else-if="selectedRegistration.status === 'PENDING'">
              患者 <strong>{{ selectedRegistration.patientName }}</strong> 待叫号，请点击「叫号」后开始书写病历
            </p>
            <p v-else>请选择一位在诊患者</p>
          </template>
        </el-empty>
      </div>
    </section>
  </div>
</template>

<style scoped>
.workbench {
  display: flex;
  height: 100%;
  background: #eef1f6;
}

.sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #dcdfe6;
  position: relative;
  transition: width 0.25s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 52px;
}

.sidebar-toggle {
  position: absolute;
  right: -14px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 20;
  width: 28px;
  height: 48px;
  border: 1px solid #dcdfe6;
  border-radius: 0 8px 8px 0;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 2px 0 6px rgba(0, 0, 0, 0.06);
  transition: color 0.2s, border-color 0.2s;
}

.sidebar-toggle:hover {
  color: #409eff;
  border-color: #409eff;
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 280px;
  height: 100%;
  overflow: hidden;
}

.sidebar-rail {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 4px;
  gap: 12px;
}

.rail-text {
  writing-mode: vertical-rl;
  font-size: 13px;
  color: #909399;
  letter-spacing: 2px;
}

.rail-count {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 12px;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.rail-current {
  writing-mode: vertical-rl;
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
}

.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid #ebeef5;
}

.tab-btn {
  flex: 1;
  padding: 12px 0;
  border: none;
  background: #f5f7fa;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #fff;
  color: #409eff;
  font-weight: 600;
  border-bottom: 2px solid #409eff;
}

.sidebar-toolbar {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
}

.patient-list {
  flex: 1;
  overflow-y: auto;
  min-height: 120px;
}

.patient-block {
  border-bottom: 1px solid #ebeef5;
}

.patient-block.active .patient-item {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}

.patient-item {
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.patient-item:hover {
  background: #f5f9ff;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-no {
  color: #409eff;
  font-weight: 600;
  font-size: 13px;
}

.name {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
}

.item-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.item-actions {
  margin-top: 8px;
}

.profile-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 0;
  border: none;
  background: none;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
}

.profile-toggle:hover {
  color: #337ecc;
}

.patient-profile-inline {
  padding: 8px 12px 12px;
  background: #fafbfc;
  border-top: 1px dashed #e4e7ed;
  font-size: 12px;
  color: #606266;
}

.profile-loading {
  color: #909399;
  text-align: center;
  padding: 8px 0;
}

.profile-row {
  margin-bottom: 6px;
  line-height: 1.5;
}

.profile-row .label {
  color: #909399;
  margin-right: 6px;
}

.profile-row .danger {
  color: #f56c6c;
  font-weight: 500;
}

.workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin: 8px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  overflow: hidden;
  background: #fff;
}

.workspace-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
