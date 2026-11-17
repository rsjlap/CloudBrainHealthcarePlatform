<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Delete, Microphone, Plus, Refresh, Search, VideoPause } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSpeechStatusApi, transcribeSpeechApi } from '@/api/ai'
import { useAudioRecorder } from '@/composables/useAudioRecorder'
import { convertRecordingToWav16k } from '@/utils/audioEncoder'
import type {
  AiMedicalDraft,
  Drug,
  PrescriptionItemRow,
  PrescriptionReviewResult,
  PrescriptionSuggestItem,
  RecommendedExam,
  RiskLevel,
} from '@/types/api'
import DoctorAiFusionStrip from './DoctorAiFusionStrip.vue'
import KgReferenceView from './KgReferenceView.vue'
import { hasValidKgReference } from '@/utils/kgReference'
import {
  draftToDisplayFields,
  extractPartialDraftFields,
  MEDICAL_STREAM_STEPS,
  resolveStreamStep,
} from '@/utils/medicalDraftStream'

export type AiSidebarMode = 'record' | 'exam' | 'rx'
export type OrderCatalogType = 'LAB' | 'EXAM'

export interface ExamCatalogItem {
  orderType: OrderCatalogType
  itemId: number
  itemCode: string
  examType: string
  bodyPart?: string
  fee?: number
}

export interface SelectedExam extends ExamCatalogItem {
  key: string
}

const props = defineProps<{
  mode: AiSidebarMode
  loading: boolean
  signed: boolean
  dialogText: string
  recordStreaming?: boolean
  medicalStreamText?: string
  streamStatus?: string
  medicalDraft: AiMedicalDraft | null
  examList: RecommendedExam[]
  examSummary?: string
  examKgAvailable?: boolean
  examKgReference?: string
  labCatalog: ExamCatalogItem[]
  examCatalog: ExamCatalogItem[]
  pendingExams: SelectedExam[]
  drugs: Drug[]
  rxItems: PrescriptionItemRow[]
  aiRxItems: PrescriptionSuggestItem[]
  rxRiskLevel?: RiskLevel
  rxAdvice?: string
  rxWarnings?: string[]
  rxReview?: PrescriptionReviewResult | null
  clinicalIndication?: string
}>()

const emit = defineEmits<{
  'update:mode': [mode: AiSidebarMode]
  'update:pendingExams': [exams: SelectedExam[]]
  'update:rxItems': [items: PrescriptionItemRow[]]
  'update:dialogText': [text: string]
  refresh: []
  'generate-record': []
  'apply-record': [draft: AiMedicalDraft]
  'review-rx': []
}>()

const editingRecord = ref(false)
const editableDraft = ref<AiMedicalDraft | null>(null)
const transcribing = ref(false)
const speechAvailable = ref(false)
const { recording, durationLabel, error: recorderError, start: startRecording, stop: stopRecording } = useAudioRecorder()

const oralDialogText = computed({
  get: () => props.dialogText,
  set: (value: string) => emit('update:dialogText', value),
})

const recordStreaming = computed(() => props.recordStreaming ?? false)

const streamStepIndex = computed(() => resolveStreamStep(props.streamStatus))
const streamPreviewFields = computed(() => extractPartialDraftFields(props.medicalStreamText || ''))
const draftPreviewFields = computed(() => draftToDisplayFields(editableDraft.value))
const examSearch = ref('')
const drugSearch = ref('')
const showExamCatalog = ref(false)
const showDrugCatalog = ref(false)

const modeTabs: { key: AiSidebarMode; label: string }[] = [
  { key: 'record', label: '病历助手' },
  { key: 'exam', label: '检查检验' },
  { key: 'rx', label: '处方' },
]

const riskTagType = computed((): 'success' | 'warning' | 'danger' | 'info' => {
  if (props.rxReview?.riskLevel) {
    const map: Record<RiskLevel, 'success' | 'warning' | 'danger'> = {
      LOW: 'success',
      MEDIUM: 'warning',
      HIGH: 'danger',
    }
    return map[props.rxReview.riskLevel]
  }
  if (!props.rxRiskLevel) return 'info'
  const map: Record<RiskLevel, 'success' | 'warning' | 'danger'> = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
  }
  return map[props.rxRiskLevel]
})

const filteredLabCatalog = computed(() => {
  const kw = examSearch.value.trim().toLowerCase()
  const list = kw
    ? props.labCatalog.filter(
        (item) =>
          item.examType.toLowerCase().includes(kw)
          || item.itemCode.toLowerCase().includes(kw)
          || (item.bodyPart && item.bodyPart.toLowerCase().includes(kw)),
      )
    : props.labCatalog
  return list.filter((item) => !isExamInPending(item))
})

const filteredExamCatalog = computed(() => {
  const kw = examSearch.value.trim().toLowerCase()
  const list = kw
    ? props.examCatalog.filter(
        (item) =>
          item.examType.toLowerCase().includes(kw)
          || item.itemCode.toLowerCase().includes(kw)
          || (item.bodyPart && item.bodyPart.toLowerCase().includes(kw)),
      )
    : props.examCatalog
  return list.filter((item) => !isExamInPending(item))
})

const allCatalogItems = computed(() => [...props.labCatalog, ...props.examCatalog])

const filteredDrugs = computed(() => {
  const kw = drugSearch.value.trim().toLowerCase()
  const list = kw
    ? props.drugs.filter(
        (d) =>
          d.drugName.toLowerCase().includes(kw)
          || (d.drugCode && d.drugCode.toLowerCase().includes(kw)),
      )
    : props.drugs
  return list.filter((d) => !isDrugInRx(d.id))
})

function catalogKey(item: ExamCatalogItem) {
  return `${item.orderType}-${item.itemId}`
}

function isExamInPending(item: ExamCatalogItem) {
  return props.pendingExams.some((e) => e.key === catalogKey(item))
}

function orderTypeLabel(orderType: OrderCatalogType) {
  return orderType === 'LAB' ? '检验' : '检查'
}

function findCatalogExam(exam: RecommendedExam) {
  if (exam.catalogMatched && exam.orderType && exam.catalogItemId) {
    return allCatalogItems.value.find(
      (c) => c.orderType === exam.orderType && c.itemId === exam.catalogItemId,
    )
  }
  const name = exam.examType?.toLowerCase() || ''
  return allCatalogItems.value.find((c) => {
    const itemName = c.examType.toLowerCase()
    return itemName === name || itemName.includes(name) || name.includes(itemName)
  })
}

function isDrugInRx(drugId: number) {
  return props.rxItems.some((r) => r.drugId === drugId)
}

function isAiExamAdopted(exam: RecommendedExam) {
  const catalog = findCatalogExam(exam)
  return catalog ? isExamInPending(catalog) : false
}

function findDrug(item: PrescriptionSuggestItem) {
  if (item.drugId) {
    const byId = props.drugs.find((d) => d.id === item.drugId)
    if (byId) return byId
  }
  return props.drugs.find(
    (d) =>
      d.drugName === item.drugName
      || (item.drugCode && d.drugCode === item.drugCode)
      || d.drugName.includes(item.drugName),
  )
}

function isAiRxAdopted(item: PrescriptionSuggestItem) {
  const drug = findDrug(item)
  return drug ? isDrugInRx(drug.id) : false
}

function addExam(item: ExamCatalogItem) {
  const key = catalogKey(item)
  if (isExamInPending(item)) return
  emit('update:pendingExams', [...props.pendingExams, { ...item, key }])
}

function removeExam(key: string) {
  emit('update:pendingExams', props.pendingExams.filter((e) => e.key !== key))
}

function adoptAiExam(exam: RecommendedExam) {
  const catalog = findCatalogExam(exam)
  if (catalog) addExam(catalog)
}

function adoptAllAiExams() {
  const next = [...props.pendingExams]
  const existingKeys = new Set(next.map((e) => e.key))
  for (const exam of props.examList) {
    const catalog = findCatalogExam(exam)
    if (!catalog) continue
    const key = catalogKey(catalog)
    if (existingKeys.has(key)) continue
    existingKeys.add(key)
    next.push({ ...catalog, key })
  }
  if (next.length !== props.pendingExams.length) {
    emit('update:pendingExams', next)
  }
}

function addDrug(drug: Drug, suggest?: PrescriptionSuggestItem) {
  if (isDrugInRx(drug.id)) return
  emit('update:rxItems', [
    ...props.rxItems,
    {
      drugId: drug.id,
      drugName: drug.drugName,
      drugCode: drug.drugCode,
      dosage: suggest?.dosage ?? 1,
      unit: suggest?.unit || drug.unit || '片',
      frequency: suggest?.frequency || '每日1次',
      durationDays: suggest?.durationDays ?? 7,
    },
  ])
}

function removeRx(index: number) {
  emit('update:rxItems', props.rxItems.filter((_, i) => i !== index))
}

function updateRxItem(index: number, field: keyof PrescriptionItemRow, value: unknown) {
  const next = props.rxItems.map((item, i) =>
    i === index ? { ...item, [field]: value } : item,
  )
  emit('update:rxItems', next)
}

function adoptAiRx(item: PrescriptionSuggestItem) {
  const drug = findDrug(item)
  if (drug) addDrug(drug, item)
}

function adoptAllAiRx() {
  const next = [...props.rxItems]
  const existingDrugIds = new Set(next.map((r) => r.drugId))
  for (const item of props.aiRxItems) {
    const drug = findDrug(item)
    if (!drug || existingDrugIds.has(drug.id)) continue
    existingDrugIds.add(drug.id)
    next.push({
      drugId: drug.id,
      drugName: drug.drugName,
      drugCode: drug.drugCode,
      dosage: item.dosage ?? 1,
      unit: item.unit || drug.unit || '片',
      frequency: item.frequency || '每日1次',
      durationDays: item.durationDays ?? 7,
    })
  }
  if (next.length !== props.rxItems.length) {
    emit('update:rxItems', next)
  }
}

function switchMode(mode: AiSidebarMode) {
  emit('update:mode', mode)
}

function onApplyRecord() {
  if (!editableDraft.value) return
  emit('apply-record', { ...editableDraft.value })
  editingRecord.value = false
}

function onGenerateRecord() {
  if (!oralDialogText.value.trim()) {
    ElMessage.warning('请先录入或转写患者口述内容')
    return
  }
  emit('generate-record')
}

async function toggleRecording() {
  if (props.signed) return
  if (recording.value) {
    const blob = await stopRecording()
    if (!blob) {
      ElMessage.warning('未录到有效音频')
      return
    }
    transcribing.value = true
    try {
      const wavBlob = await convertRecordingToWav16k(blob)
      const result = await transcribeSpeechApi(wavBlob)
      if (result.text?.trim()) {
        oralDialogText.value = oralDialogText.value
          ? `${oralDialogText.value.trim()}\n${result.text.trim()}`
          : result.text.trim()
        ElMessage.success('语音转写完成')
      } else {
        ElMessage.warning('未识别到有效语音内容')
      }
    } catch {
      ElMessage.warning('语音转写失败，请检查麦克风权限或科大讯飞配置')
    } finally {
      transcribing.value = false
    }
    return
  }

  try {
    await startRecording()
  } catch {
    ElMessage.warning(recorderError.value || '无法启动录音，请检查麦克风权限')
  }
}

onMounted(async () => {
  try {
    const status = await getSpeechStatusApi()
    speechAvailable.value = !!status.speechAvailable
  } catch {
    speechAvailable.value = false
  }
})

watch(
  () => props.medicalDraft,
  (draft) => {
    editableDraft.value = draft ? { ...draft } : null
  },
  { immediate: true, deep: true },
)

watch(
  () => props.mode,
  (mode) => {
    if (mode === 'exam') showExamCatalog.value = false
    if (mode === 'rx') showDrugCatalog.value = false
  },
)
</script>

<template>
  <aside class="ai-sidebar">
    <div class="ai-header">
      <div class="ai-title">
        <span class="ai-icon">AI</span>
        <span>智能助手</span>
      </div>
      <el-button
        :icon="Refresh"
        size="small"
        text
        :loading="loading"
        :disabled="signed"
        @click="emit('refresh')"
      />
    </div>

    <div class="ai-tabs">
      <button
        v-for="tab in modeTabs"
        :key="tab.key"
        class="ai-tab"
        :class="{ active: mode === tab.key }"
        @click="switchMode(tab.key)"
      >
        {{ tab.label }}
        <span v-if="tab.key === 'exam' && pendingExams.length" class="tab-badge">{{ pendingExams.length }}</span>
        <span v-if="tab.key === 'rx' && rxItems.length" class="tab-badge">{{ rxItems.length }}</span>
      </button>
    </div>

    <div v-loading="loading" class="ai-body">
      <!-- 病历助手 -->
      <template v-if="mode === 'record'">
        <div class="oral-section">
          <div class="section-head">
            <span class="section-title">患者口述</span>
            <span v-if="recording" class="rec-status">{{ durationLabel() }}</span>
          </div>
          <el-input
            v-model="oralDialogText"
            type="textarea"
            :rows="5"
            :disabled="signed || transcribing"
            placeholder="可直接录入患者口述，或点击录音后自动转写为文字"
          />
          <div class="oral-actions">
            <el-button
              v-if="speechAvailable"
              :type="recording ? 'danger' : 'default'"
              size="small"
              :icon="recording ? VideoPause : Microphone"
              :disabled="signed || transcribing"
              @click="toggleRecording"
            >
              {{ recording ? '结束录音' : '开始录音' }}
            </el-button>
            <el-button
              type="primary"
              size="small"
              :loading="loading || recordStreaming"
              :disabled="signed || !oralDialogText.trim() || recordStreaming"
              @click="onGenerateRecord"
            >
              AI 解析病历
            </el-button>
          </div>
          <p v-if="transcribing" class="oral-hint">正在调用科大讯飞转写…</p>
          <p v-else-if="!speechAvailable" class="oral-hint">语音识别未配置，可直接输入口述文本后解析</p>
          <p v-else class="oral-hint">录音结束后将自动转写，确认文字无误后再点「AI 解析病历」</p>
        </div>

        <div v-if="recordStreaming" class="stream-panel">
          <div class="stream-head">
            <span class="stream-title">AI 病历生成中</span>
            <span v-if="streamStatus" class="stream-status">{{ streamStatus }}</span>
          </div>
          <div class="stream-steps">
            <div
              v-for="(step, index) in MEDICAL_STREAM_STEPS"
              :key="step"
              class="stream-step"
              :class="{
                done: index < streamStepIndex,
                active: index === streamStepIndex,
              }"
            >
              <span class="step-dot">{{ index + 1 }}</span>
              <span class="step-text">{{ step }}</span>
            </div>
          </div>
          <div v-if="streamPreviewFields.length" class="ai-preview stream-preview">
            <div
              v-for="field in streamPreviewFields"
              :key="`stream-${field.key}`"
              class="preview-item"
            >
              <span class="label">{{ field.label }}</span>
              <p>{{ field.value }}</p>
            </div>
          </div>
          <p v-else class="stream-hint">正在分析患者口述，请稍候…</p>
        </div>

        <div v-else-if="!medicalDraft" class="ai-empty">
          <p>暂无 AI 病历建议</p>
          <p class="muted">录入患者口述后，点击「AI 解析病历」生成草稿</p>
        </div>
        <template v-else>
          <DoctorAiFusionStrip
            title="病历生成流程"
            :ai-available="medicalDraft.aiAvailable"
            :kg-available="medicalDraft.kgAvailable"
            :steps="['患者口述', 'LLM 草稿', 'KG 校验术语', '预览确认']"
          />
          <div class="ai-meta">
            <el-button
              v-if="!signed"
              size="small"
              text
              type="primary"
              @click="editingRecord = !editingRecord"
            >
              {{ editingRecord ? '预览' : '修改' }}
            </el-button>
          </div>
          <el-collapse v-if="hasValidKgReference(medicalDraft.kgReference)" class="kg-ref-collapse">
            <el-collapse-item title="知识图谱校验参考" name="kg">
              <KgReferenceView :content="medicalDraft.kgReference" />
            </el-collapse-item>
          </el-collapse>
          <div v-if="editingRecord && editableDraft" class="ai-edit-form">
            <label>主诉</label>
            <el-input v-model="editableDraft.chiefComplaint" type="textarea" :rows="2" />
            <label>现病史</label>
            <el-input v-model="editableDraft.presentIllness" type="textarea" :rows="3" />
            <label>诊断</label>
            <el-input v-model="editableDraft.diagnosis" />
            <label>查体</label>
            <el-input v-model="editableDraft.physicalExam" type="textarea" :rows="2" />
            <label>处理意见</label>
            <el-input v-model="editableDraft.treatmentPlan" type="textarea" :rows="2" />
          </div>
          <div v-else-if="editableDraft" class="ai-preview">
            <div
              v-for="field in draftPreviewFields"
              :key="`draft-${field.key}`"
              class="preview-item"
            >
              <span class="label">{{ field.label }}</span>
              <p>{{ field.value }}</p>
            </div>
            <p v-if="!draftPreviewFields.length" class="muted">暂无可用字段，请重新生成</p>
          </div>
          <el-button
            v-if="!signed"
            type="primary"
            class="apply-btn"
            :disabled="!editableDraft"
            @click="onApplyRecord"
          >
            一键填入病历
          </el-button>
        </template>
      </template>

      <!-- 检查 -->
      <template v-else-if="mode === 'exam'">
        <DoctorAiFusionStrip
          title="检查推荐流程"
          :ai-available="examList.length > 0"
          :kg-available="examKgAvailable"
          :steps="['病情分析', 'LLM 初荐', 'KG 标准项', '融合报告']"
        />
        <div class="section-block">
          <div class="section-head">
            <span class="section-title">融合推荐报告</span>
            <el-button
              v-if="!signed && examList.length"
              size="small"
              text
              type="primary"
              @click="adoptAllAiExams"
            >
              全部采纳
            </el-button>
          </div>
          <p v-if="examSummary" class="ai-summary fusion-summary">{{ examSummary }}</p>
          <el-collapse v-if="hasValidKgReference(examKgReference)" class="kg-ref-collapse">
            <el-collapse-item title="知识图谱标准检查项" name="kg">
              <KgReferenceView :content="examKgReference" />
            </el-collapse-item>
          </el-collapse>
          <div v-if="loading && !examList.length" class="mini-empty">正在融合 AI 与知识图谱…</div>
          <div v-else-if="!examList.length" class="mini-empty">暂无 AI 推荐，可手动搜索添加</div>
          <div
            v-for="(exam, idx) in examList"
            :key="`ai-exam-${idx}`"
            class="ai-suggest-card"
          >
            <div class="suggest-main">
              <div class="card-title">{{ exam.examType }} · {{ exam.bodyPart }}</div>
              <p v-if="exam.clinicalIndication" class="card-desc">{{ exam.clinicalIndication }}</p>
            </div>
            <el-button
              v-if="!signed"
              size="small"
              :type="isAiExamAdopted(exam) ? 'success' : 'primary'"
              :plain="isAiExamAdopted(exam)"
              :disabled="isAiExamAdopted(exam) || !findCatalogExam(exam)"
              @click="adoptAiExam(exam)"
            >
              {{ isAiExamAdopted(exam) ? '已添加' : findCatalogExam(exam) ? '添加' : '目录无匹配' }}
            </el-button>
          </div>
        </div>

        <div class="section-block">
          <div class="section-head">
            <span class="section-title">已选项目（{{ pendingExams.length }}）</span>
          </div>
          <div v-if="!pendingExams.length" class="mini-empty">暂未选择检查/检验</div>
          <div v-for="exam in pendingExams" :key="exam.key" class="selected-card">
            <div class="selected-main">
              <div class="card-title">
                <el-tag size="small" :type="exam.orderType === 'LAB' ? 'success' : 'warning'" class="type-tag">
                  {{ orderTypeLabel(exam.orderType) }}
                </el-tag>
                {{ exam.examType }} · {{ exam.bodyPart || '-' }}
              </div>
              <p class="card-desc">{{ clinicalIndication || '待签名后开立' }}</p>
            </div>
            <el-button
              v-if="!signed"
              :icon="Delete"
              size="small"
              text
              type="danger"
              @click="removeExam(exam.key)"
            />
          </div>
        </div>

        <div v-if="!signed" class="section-block">
          <div class="section-head">
            <span class="section-title">搜索添加</span>
            <el-button size="small" text type="primary" @click="showExamCatalog = !showExamCatalog">
              {{ showExamCatalog ? '收起' : '展开' }}
            </el-button>
          </div>
          <el-collapse-transition>
            <div v-show="showExamCatalog">
              <el-input
                v-model="examSearch"
                placeholder="搜索名称或编码"
                :prefix-icon="Search"
                clearable
                size="small"
                class="search-input"
              />
              <div v-if="filteredLabCatalog.length" class="catalog-group">
                <div class="catalog-group-title">检验项目</div>
                <div class="catalog-list">
                  <div
                    v-for="item in filteredLabCatalog"
                    :key="`lab-${item.itemId}`"
                    class="catalog-row"
                  >
                    <div>
                      <div class="catalog-name">{{ item.examType }}</div>
                      <div class="catalog-sub">{{ item.bodyPart || item.itemCode }} · ¥{{ item.fee ?? '-' }}</div>
                    </div>
                    <el-button :icon="Plus" size="small" circle @click="addExam(item)" />
                  </div>
                </div>
              </div>
              <div v-if="filteredExamCatalog.length" class="catalog-group">
                <div class="catalog-group-title">检查项目</div>
                <div class="catalog-list">
                  <div
                    v-for="item in filteredExamCatalog"
                    :key="`exam-${item.itemId}`"
                    class="catalog-row"
                  >
                    <div>
                      <div class="catalog-name">{{ item.examType }}</div>
                      <div class="catalog-sub">{{ item.bodyPart || '-' }} · {{ item.itemCode }} · ¥{{ item.fee ?? '-' }}</div>
                    </div>
                    <el-button :icon="Plus" size="small" circle @click="addExam(item)" />
                  </div>
                </div>
              </div>
              <div v-if="!filteredLabCatalog.length && !filteredExamCatalog.length" class="mini-empty">无匹配项目</div>
            </div>
          </el-collapse-transition>
        </div>
      </template>

      <!-- 处方 -->
      <template v-else>
        <DoctorAiFusionStrip
          title="处方审核流程"
          :ai-available="!!rxReview?.aiAvailable"
          :kg-available="!!rxReview?.kgAvailable"
          :steps="['提交药品', 'LLM 相互作用', 'KG 禁忌', '综合报告']"
        />
        <div v-if="!signed" class="review-action">
          <el-button
            type="warning"
            size="small"
            :loading="loading"
            :disabled="!rxItems.length"
            @click="emit('review-rx')"
          >
            AI + KG 处方审核
          </el-button>
        </div>
        <div v-if="rxReview" class="section-block review-block">
          <div class="section-head">
            <span class="section-title">融合审核报告</span>
            <el-tag :type="riskTagType" size="small">{{ rxReview.riskLevel }}</el-tag>
          </div>
          <p class="ai-summary fusion-summary">{{ rxReview.overallAdvice }}</p>
          <ul v-if="rxReview.warnings?.length" class="warning-list">
            <li v-for="(w, i) in rxReview.warnings" :key="`rw-${i}`">{{ w }}</li>
          </ul>
          <div v-if="rxReview.interactions.length" class="interaction-list">
            <div v-for="(it, i) in rxReview.interactions" :key="`ri-${i}`" class="interaction-item">
              <el-tag :type="it.severity === 'HIGH' ? 'danger' : it.severity === 'MEDIUM' ? 'warning' : 'info'" size="small">
                {{ it.severity }}
              </el-tag>
              <span>{{ it.drug1 }} + {{ it.drug2 }}：{{ it.description }}</span>
            </div>
          </div>
          <p v-if="rxReview.inventoryAdvice" class="inventory-hint">{{ rxReview.inventoryAdvice }}</p>
          <el-collapse v-if="hasValidKgReference(rxReview.kgReference)" class="kg-ref-collapse">
            <el-collapse-item title="知识图谱禁忌参考" name="kg">
              <KgReferenceView :content="rxReview.kgReference" />
            </el-collapse-item>
          </el-collapse>
        </div>
        <div class="section-block">
          <div class="section-head">
            <span class="section-title">AI 用药推荐</span>
            <el-button
              v-if="!signed && aiRxItems.length"
              size="small"
              text
              type="primary"
              @click="adoptAllAiRx"
            >
              全部采纳
            </el-button>
          </div>
          <div class="ai-meta" v-if="rxRiskLevel || rxAdvice">
            <el-tag v-if="rxRiskLevel" :type="riskTagType" size="small">风险 {{ rxRiskLevel }}</el-tag>
          </div>
          <p v-if="rxAdvice" class="ai-summary">{{ rxAdvice }}</p>
          <ul v-if="rxWarnings?.length" class="warning-list">
            <li v-for="(w, i) in rxWarnings" :key="i">{{ w }}</li>
          </ul>
          <div v-if="loading && !aiRxItems.length" class="mini-empty">正在生成推荐…</div>
          <div v-else-if="!aiRxItems.length" class="mini-empty">暂无 AI 推荐，可手动搜索添加</div>
          <div
            v-for="(item, idx) in aiRxItems"
            :key="`ai-rx-${idx}`"
            class="ai-suggest-card"
          >
            <div class="suggest-main">
              <div class="card-title">{{ item.drugName }}</div>
              <p class="card-desc">
                {{ item.dosage }}{{ item.unit }} {{ item.frequency }} × {{ item.durationDays }}天
              </p>
              <p v-if="item.reason" class="card-reason">{{ item.reason }}</p>
            </div>
            <el-button
              v-if="!signed"
              size="small"
              :type="isAiRxAdopted(item) ? 'success' : 'primary'"
              :plain="isAiRxAdopted(item)"
              :disabled="isAiRxAdopted(item) || !findDrug(item)"
              @click="adoptAiRx(item)"
            >
              {{ isAiRxAdopted(item) ? '已添加' : findDrug(item) ? '添加' : '目录无匹配' }}
            </el-button>
          </div>
        </div>

        <div class="section-block">
          <div class="section-head">
            <span class="section-title">已选药品（{{ rxItems.length }}）</span>
          </div>
          <div v-if="!rxItems.length" class="mini-empty">暂未选择药品</div>
          <div v-for="(item, index) in rxItems" :key="`${item.drugId}-${index}`" class="selected-card rx-selected">
            <div class="selected-main">
              <div class="card-title">{{ item.drugName }}</div>
              <div v-if="!signed" class="rx-fields">
                <el-input-number
                  :model-value="item.dosage"
                  :min="0.5"
                  :step="0.5"
                  size="small"
                  controls-position="right"
                  @update:model-value="(v: number) => updateRxItem(index, 'dosage', v)"
                />
                <el-input
                  :model-value="item.unit"
                  size="small"
                  style="width: 52px"
                  @update:model-value="(v: string) => updateRxItem(index, 'unit', v)"
                />
                <el-input
                  :model-value="item.frequency"
                  size="small"
                  placeholder="频次"
                  @update:model-value="(v: string) => updateRxItem(index, 'frequency', v)"
                />
                <el-input-number
                  :model-value="item.durationDays"
                  :min="1"
                  size="small"
                  controls-position="right"
                  @update:model-value="(v: number) => updateRxItem(index, 'durationDays', v)"
                />
                <span class="unit-hint">天</span>
              </div>
              <p v-else class="card-desc">
                {{ item.dosage }}{{ item.unit }} {{ item.frequency }} × {{ item.durationDays }}天
              </p>
            </div>
            <el-button
              v-if="!signed"
              :icon="Delete"
              size="small"
              text
              type="danger"
              @click="removeRx(index)"
            />
          </div>
        </div>

        <div v-if="!signed" class="section-block">
          <div class="section-head">
            <span class="section-title">搜索添加</span>
            <el-button size="small" text type="primary" @click="showDrugCatalog = !showDrugCatalog">
              {{ showDrugCatalog ? '收起' : '展开' }}
            </el-button>
          </div>
          <el-collapse-transition>
            <div v-show="showDrugCatalog">
              <el-input
                v-model="drugSearch"
                placeholder="搜索药品名称或编码"
                :prefix-icon="Search"
                clearable
                size="small"
                class="search-input"
              />
              <div class="catalog-list">
                <div v-for="drug in filteredDrugs" :key="drug.id" class="catalog-row">
                  <div>
                    <div class="catalog-name">{{ drug.drugName }}</div>
                    <div class="catalog-sub">{{ drug.drugCode }} · {{ drug.specification }} · ¥{{ drug.unitPrice ?? '-' }}</div>
                  </div>
                  <el-button :icon="Plus" size="small" circle @click="addDrug(drug)" />
                </div>
                <div v-if="!filteredDrugs.length" class="mini-empty">无匹配药品</div>
              </div>
            </div>
          </el-collapse-transition>
        </div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.ai-sidebar {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e4e7ed;
  background: #f8fafc;
  height: 100%;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e4e7ed;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.ai-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  font-weight: 700;
}

.ai-header :deep(.el-button) {
  color: #fff;
}

.ai-tabs {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.ai-tab {
  flex: 1;
  padding: 10px 4px;
  border: none;
  background: none;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  position: relative;
}

.ai-tab.active {
  color: #667eea;
  font-weight: 600;
  border-bottom-color: #667eea;
}

.tab-badge {
  display: inline-block;
  margin-left: 2px;
  padding: 0 5px;
  font-size: 11px;
  background: #667eea;
  color: #fff;
  border-radius: 8px;
  line-height: 16px;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  min-height: 200px;
}

.ai-empty,
.mini-empty {
  text-align: center;
  padding: 16px 8px;
  color: #909399;
  font-size: 12px;
}

.ai-empty {
  padding: 32px 12px;
  font-size: 13px;
}

.ai-empty .muted {
  font-size: 12px;
  margin-top: 8px;
}

.section-block {
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.ai-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.ai-summary {
  font-size: 12px;
  color: #606266;
  background: #fff;
  padding: 8px 10px;
  border-radius: 4px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.warning-list {
  margin: 0 0 8px;
  padding-left: 18px;
  font-size: 12px;
  color: #e6a23c;
}

.ai-suggest-card,
.selected-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.ai-suggest-card {
  border-color: #d4daf7;
  background: #fafbff;
}

.selected-card {
  border-color: #c6e2ff;
  background: #f5f9ff;
}

.suggest-main,
.selected-main {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}

.card-desc,
.card-reason {
  font-size: 12px;
  color: #909399;
  margin: 0;
  line-height: 1.4;
}

.search-input {
  margin-bottom: 8px;
}

.oral-section {
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #ebeef5;
}

.oral-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.rec-status {
  font-size: 12px;
  color: #f56c6c;
  font-variant-numeric: tabular-nums;
}

.oral-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.stream-panel {
  margin-bottom: 14px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.stream-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.stream-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.stream-status {
  font-size: 12px;
  color: #409eff;
}

.stream-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.stream-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #ebeef5;
  color: #909399;
  font-size: 12px;
}

.stream-step.active {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.stream-step.done {
  border-color: #b3e19d;
  color: #67c23a;
  background: #f0f9eb;
}

.step-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  background: #f2f3f5;
}

.stream-step.active .step-dot {
  background: #409eff;
  color: #fff;
}

.stream-step.done .step-dot {
  background: #67c23a;
  color: #fff;
}

.stream-preview {
  margin-top: 4px;
}

.stream-hint {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.catalog-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}

.catalog-group {
  margin-top: 10px;
}

.catalog-group-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}

.type-tag {
  margin-right: 6px;
  vertical-align: middle;
}

.catalog-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid #f0f2f5;
}

.catalog-row:last-child {
  border-bottom: none;
}

.catalog-name {
  font-size: 13px;
  color: #303133;
}

.catalog-sub {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.rx-fields {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}

.rx-selected {
  flex-direction: column;
  align-items: stretch;
}

.rx-selected .selected-main {
  width: 100%;
}

.rx-selected > .el-button {
  align-self: flex-end;
}

.unit-hint {
  font-size: 12px;
  color: #909399;
}

.ai-preview .preview-item {
  margin-bottom: 12px;
  background: #fff;
  padding: 8px 10px;
  border-radius: 4px;
}

.preview-item .label {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.preview-item p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
  white-space: pre-wrap;
}

.ai-edit-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-edit-form label {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.apply-btn {
  width: 100%;
  margin-top: 12px;
}

.fusion-summary {
  border-left: 3px solid #667eea;
}

.kg-ref-collapse {
  margin-bottom: 10px;
}

.review-action {
  margin-bottom: 12px;
}

.review-block {
  border: 1px solid #fde2e2;
  background: #fffbfb;
  border-radius: 6px;
  padding: 10px;
}

.interaction-list {
  margin-bottom: 8px;
}

.interaction-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
  line-height: 1.4;
}

.inventory-hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px;
}
</style>
