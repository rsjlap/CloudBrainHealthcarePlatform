<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewPrescriptionApi, generateMedicalRecordStreamApi, recommendExamsApi, suggestPrescriptionApi, refineTreatmentPlanApi } from '@/api/ai'
import { listExamItemsApi, listLabItemsApi } from '@/api/catalog'
import { createExamOrderApi, getExamReportByOrderApi, listExamOrdersByRegistrationApi, type ExamOrder, type ExamReport } from '@/api/exam'
import { listDrugsApi } from '@/api/drug'
import {
  confirmMedicalRecordApi,
  getMedicalRecordByRegistrationApi,
  saveMedicalRecordApi,
} from '@/api/medicalRecord'
import { createPrescriptionApi, listPrescriptionsByRegistrationApi } from '@/api/prescription'
import type {
  AiMedicalDraft,
  Drug,
  Prescription,
  PrescriptionItemRow,
  PrescriptionReviewResult,
  PrescriptionSuggestItem,
  RecommendedExam,
  Registration,
  RegistrationPatientInfo,
  RiskLevel,
} from '@/types/api'
import { useUserStore } from '@/stores/user'
import { DOCTOR_ALERT_EVENT } from '@/types/websocket'
import type { DoctorAlertMessage } from '@/types/websocket'
import { normalizeMedicalDraft } from '@/utils/medicalDraftStream'
import DoctorAiSidebar, { type AiSidebarMode, type ExamCatalogItem, type SelectedExam } from './DoctorAiSidebar.vue'
import ExamReportDetailDialog from './ExamReportDetailDialog.vue'

const props = defineProps<{
  registrationId?: number
  registration: Registration | undefined
  patientInfo: RegistrationPatientInfo | null
}>()

const emit = defineEmits<{
  complete: []
}>()

const userStore = useUserStore()

const labCatalog = ref<ExamCatalogItem[]>([])
const examCatalog = ref<ExamCatalogItem[]>([])

const visitType = ref<'initial' | 'followup'>('initial')
const headerCollapsed = ref(false)
const signed = ref(false)
const signing = ref(false)
const completing = ref(false)
const currentRecordId = ref<number>()
const drugs = ref<Drug[]>([])
const pendingExams = ref<SelectedExam[]>([])
const examOrders = ref<ExamOrder[]>([])
const examReports = ref<Record<number, ExamReport>>({})
const examReportDialogVisible = ref(false)
const viewingExamOrder = ref<ExamOrder | null>(null)
const viewingExamReport = ref<ExamReport | null>(null)
const rxItems = ref<PrescriptionItemRow[]>([])
const savedPrescriptions = ref<Prescription[]>([])
const signedExamSnapshot = ref<SelectedExam[]>([])
let loadRecordToken = 0
const pollTimer = ref<number>()
const treatmentPlanRefined = ref(false)
const refiningTreatmentPlan = ref(false)
const signedTreatmentPlan = ref('')

function signedPlanStorageKey(registrationId: number) {
  return `cbm_signed_plan_${registrationId}`
}

function rememberSignedTreatmentPlan(registrationId: number, plan: string) {
  signedTreatmentPlan.value = plan
  sessionStorage.setItem(signedPlanStorageKey(registrationId), plan)
}

function restoreSignedTreatmentPlan(registrationId: number, fallback = '') {
  const stored = sessionStorage.getItem(signedPlanStorageKey(registrationId))
  signedTreatmentPlan.value = stored ?? fallback
  return signedTreatmentPlan.value
}

function syncTreatmentPlanRefinedFlag(currentPlan: string) {
  const baseline = signedTreatmentPlan.value.trim()
  const current = currentPlan.trim()
  treatmentPlanRefined.value = !!baseline && !!current && current !== baseline
}
const showExamEditor = ref(false)
const showRxEditor = ref(false)
const examPicker = ref('')

const aiMode = ref<AiSidebarMode>('record')
const aiLoading = ref(false)
const aiMedicalDraft = ref<AiMedicalDraft | null>(null)
const aiExamList = ref<RecommendedExam[]>([])
const aiExamSummary = ref('')
const aiExamKgAvailable = ref(false)
const aiExamKgReference = ref('')
const aiRxItems = ref<PrescriptionSuggestItem[]>([])
const aiRxRiskLevel = ref<RiskLevel>()
const aiRxAdvice = ref('')
const aiRxWarnings = ref<string[]>([])
const aiRxReview = ref<PrescriptionReviewResult | null>(null)
const oralDialogText = ref('')
const recordAiApplied = ref(false)
const recordStreaming = ref(false)
const medicalStreamText = ref('')
const recordStreamStatus = ref('')
let medicalStreamAbort: AbortController | null = null

const form = reactive({
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  allergyHistory: '',
  personalHistory: '',
  maritalHistory: '',
  familyHistory: '',
  temperature: '',
  respiration: '',
  pulse: '',
  bloodPressure: '',
  physicalExam: '',
  auxiliaryExam: '',
  diagnosis: '',
  treatmentPlan: '',
})

const genderLabel = (gender?: number) => {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '-'
}

type DisplayExamRow = {
  id: number | string
  orderType?: 'LAB' | 'EXAM'
  examType: string
  bodyPart?: string
  status: string
  paid?: boolean
  isPending?: boolean
}

const prescriptionStatusLabel: Record<string, string> = {
  PENDING_DISPENSE: '待发药',
  DISPENSED: '已发药',
}

const latestPrescriptionStatus = computed(() => savedPrescriptions.value[0]?.status)

const orderTypeLabel = (orderType?: 'LAB' | 'EXAM') => (orderType === 'LAB' ? '检验' : '检查')

const displayExams = computed<DisplayExamRow[]>(() => {
  const pendingRows: DisplayExamRow[] = pendingExams.value.map((e) => ({
    id: `pending-${e.key}`,
    orderType: e.orderType,
    examType: e.examType,
    bodyPart: e.bodyPart,
    status: 'SELECTED',
    paid: false,
    isPending: true,
  }))
  return [...pendingRows, ...examOrders.value.map((o) => ({ ...o, isPending: false }))]
})

const visibleExamRows = computed<DisplayExamRow[]>(() => {
  if (signed.value) {
    if (examOrders.value.length) {
      return examOrders.value.map((o) => ({ ...o, isPending: false }))
    }
    return signedExamSnapshot.value.map((e) => ({
      id: e.key,
      orderType: e.orderType,
      examType: e.examType,
      bodyPart: e.bodyPart,
      status: 'PENDING',
      paid: false,
      isPending: false,
    }))
  }
  return displayExams.value
})

const examCount = computed(() => visibleExamRows.value.length)

async function loadCatalog() {
  try {
    const [labItems, examItems] = await Promise.all([listLabItemsApi(), listExamItemsApi()])
    labCatalog.value = labItems.map((item) => ({
      orderType: 'LAB' as const,
      itemId: item.id,
      itemCode: item.itemCode,
      examType: item.itemName,
      bodyPart: item.category,
      fee: item.fee,
    }))
    examCatalog.value = examItems.map((item) => ({
      orderType: 'EXAM' as const,
      itemId: item.id,
      itemCode: item.itemCode,
      examType: item.itemName,
      bodyPart: item.bodyPart,
      fee: item.fee,
    }))
  } catch {
    ElMessage.warning('检查/检验目录加载失败')
  }
}

const allExamsFinished = computed(() => {
  if (examOrders.value.length === 0) {
    if (signed.value && signedExamSnapshot.value.length > 0) return false
    return true
  }
  return examOrders.value.every((o) => o.status === 'DONE' && examReports.value[o.id])
})

const canComplete = computed(() => signed.value && allExamsFinished.value)

const examStatusLabel: Record<string, string> = {
  SELECTED: '已选待提交',
  PENDING: '待检查',
  IN_PROGRESS: '检查中',
  DONE: '已完成',
}

function resetState() {
  visitType.value = 'initial'
  signed.value = false
  currentRecordId.value = undefined
  pendingExams.value = []
  examOrders.value = []
  examReports.value = {}
  rxItems.value = []
  savedPrescriptions.value = []
  signedExamSnapshot.value = []
  form.chiefComplaint = ''
  form.presentIllness = ''
  form.pastHistory = ''
  form.allergyHistory = ''
  form.personalHistory = ''
  form.maritalHistory = ''
  form.familyHistory = ''
  form.temperature = ''
  form.respiration = ''
  form.pulse = ''
  form.bloodPressure = ''
  form.physicalExam = ''
  form.auxiliaryExam = ''
  form.diagnosis = ''
  form.treatmentPlan = ''
  aiMedicalDraft.value = null
  aiExamList.value = []
  aiExamSummary.value = ''
  aiExamKgAvailable.value = false
  aiExamKgReference.value = ''
  aiRxItems.value = []
  aiRxRiskLevel.value = undefined
  aiRxAdvice.value = ''
  aiRxWarnings.value = []
  aiRxReview.value = null
  oralDialogText.value = ''
  recordAiApplied.value = false
  recordStreaming.value = false
  medicalStreamText.value = ''
  recordStreamStatus.value = ''
  medicalStreamAbort?.abort()
  medicalStreamAbort = null
  aiMode.value = 'record'
  treatmentPlanRefined.value = false
  refiningTreatmentPlan.value = false
  signedTreatmentPlan.value = ''
  showExamEditor.value = false
  showRxEditor.value = false
  examPicker.value = ''
  stopPolling()
}

function applyPatientDefaults() {
  form.chiefComplaint = props.registration?.chiefComplaint || ''
  form.allergyHistory = props.patientInfo?.allergyHistory || '无'
  form.pastHistory = props.patientInfo?.medicalHistory || ''
  if (!oralDialogText.value.trim() && props.registration?.chiefComplaint) {
    oralDialogText.value = props.registration.chiefComplaint
  }
}

function buildPhysicalExamText() {
  const parts: string[] = []
  if (form.temperature) parts.push(`体温 ${form.temperature}℃`)
  if (form.respiration) parts.push(`呼吸 ${form.respiration}次/分`)
  if (form.pulse) parts.push(`脉搏 ${form.pulse}次/分`)
  if (form.bloodPressure) parts.push(`血压 ${form.bloodPressure}mmHg`)
  const vitals = parts.length ? parts.join('；') : ''
  return [vitals, form.physicalExam].filter(Boolean).join('\n')
}

async function loadExamOrderList() {
  if (!props.registrationId) return
  const orders = await listExamOrdersByRegistrationApi(props.registrationId)
  examOrders.value = Array.isArray(orders) ? orders : []
  if (examOrders.value.length) {
    signedExamSnapshot.value = []
  }
}

async function loadExamReports() {
  const reports: Record<number, ExamReport> = {}
  const reportTexts: string[] = []
  for (const order of examOrders.value) {
    if (order.status !== 'DONE') continue
    try {
      const report = await getExamReportByOrderApi(order.id)
      if (report) {
        reports[order.id] = report
        const parts = [`【${order.examType}】`]
        if (report.reportText) parts.push(report.reportText)
        if (report.conclusion) parts.push(`结论：${report.conclusion}`)
        reportTexts.push(parts.join('\n'))
      }
    } catch {
      // 单个报告加载失败不影响检查单列表展示
    }
  }
  examReports.value = reports
  if (reportTexts.length) {
    form.auxiliaryExam = reportTexts.join('\n\n')
  }
  await tryRefineTreatmentPlan()
}

async function loadPrescriptions() {
  if (!props.registrationId) return
  const prescriptions = await listPrescriptionsByRegistrationApi(props.registrationId)
  savedPrescriptions.value = prescriptions
  if (prescriptions.length === 0) {
    if (signed.value) rxItems.value = []
    return
  }
  rxItems.value = prescriptions.flatMap((rx) =>
    rx.items.map((item) => ({
      drugId: item.drugId,
      drugName: item.drugName,
      drugCode: item.drugCode,
      dosage: item.dosage,
      unit: item.unit,
      frequency: item.frequency,
      durationDays: item.durationDays,
      totalAmount: item.totalAmount,
    })),
  )
}

async function tryRefineTreatmentPlan() {
  if (signed.value && examOrders.value.length > 0 && allExamsFinished.value) {
    await maybeRefineTreatmentPlan()
  }
}

async function persistTreatmentPlan(treatmentPlan: string) {
  if (!props.registrationId) return
  const record = await saveMedicalRecordApi({
    registrationId: props.registrationId,
    dialogText: oralDialogText.value.trim() || undefined,
    chiefComplaint: form.chiefComplaint,
    presentIllness: form.presentIllness,
    pastHistory: [form.pastHistory, form.allergyHistory ? `过敏史：${form.allergyHistory}` : '']
      .filter(Boolean)
      .join('\n'),
    physicalExam: buildPhysicalExamText(),
    diagnosis: form.diagnosis,
    treatmentPlan,
    status: 'CONFIRMED',
    isAiGenerated: 1,
  }, { skipErrorToast: true })
  currentRecordId.value = record.id
}

async function maybeRefineTreatmentPlan(force = false) {
  if (!signed.value || refiningTreatmentPlan.value) return
  if (!force && treatmentPlanRefined.value) return
  if (!props.registrationId || examOrders.value.length === 0 || !allExamsFinished.value) return

  refiningTreatmentPlan.value = true
  try {
    const result = await refineTreatmentPlanApi({
      registrationId: props.registrationId,
      treatmentPlan: signedTreatmentPlan.value || form.treatmentPlan,
      diagnosis: form.diagnosis,
    })
    if (!result?.treatmentPlan?.trim()) {
      ElMessage.warning('AI 未返回有效处理意见，请手动填写')
      return
    }
    form.treatmentPlan = result.treatmentPlan.trim()
    try {
      await persistTreatmentPlan(form.treatmentPlan)
    } catch {
      ElMessage.warning('处理意见已更新，但保存失败，结束看诊时将再次保存')
      return
    }
    treatmentPlanRefined.value = true
    ElMessage.success(result.aiAvailable ? 'AI 已结合检查结果优化处理意见并已保存' : '已根据检查结果更新处理意见并已保存')
  } catch (err) {
    const msg = err instanceof Error ? err.message : ''
    ElMessage.warning(msg && msg !== '请求失败' ? `处理意见优化失败：${msg}` : '处理意见优化失败，请稍后重试')
  } finally {
    refiningTreatmentPlan.value = false
  }
}

function openExamReport(order: ExamOrder) {
  const report = examReports.value[order.id]
  if (!report) return
  viewingExamOrder.value = order
  viewingExamReport.value = report
  examReportDialogVisible.value = true
}

async function loadRecord() {
  stopPolling()
  if (!props.registrationId) {
    resetState()
    return
  }

  const token = ++loadRecordToken
  resetState()
  applyPatientDefaults()

  try {
    const record = await getMedicalRecordByRegistrationApi(props.registrationId)
    if (token !== loadRecordToken) return
    if (record) {
      currentRecordId.value = record.id
      form.chiefComplaint = record.chiefComplaint || form.chiefComplaint
      form.presentIllness = record.presentIllness || ''
      form.pastHistory = record.pastHistory || form.pastHistory
      form.physicalExam = record.physicalExam || ''
      form.diagnosis = record.diagnosis || ''
      form.treatmentPlan = record.treatmentPlan || ''
      oralDialogText.value = record.dialogText || oralDialogText.value
      signed.value = record.status === 'CONFIRMED'
      if (signed.value) {
        restoreSignedTreatmentPlan(props.registrationId, record.treatmentPlan || '')
        syncTreatmentPlanRefinedFlag(record.treatmentPlan || '')
        showExamEditor.value = false
        showRxEditor.value = false
      }
    }
  } catch {
    if (token !== loadRecordToken) return
    ElMessage.warning('病历加载失败')
  }

  try {
    await loadExamOrderList()
  } catch {
    if (token !== loadRecordToken) return
    ElMessage.warning('检查/检验列表加载失败')
  }

  if (token !== loadRecordToken) return

  try {
    await loadExamReports()
  } catch {
    // 报告加载失败不影响检查单展示
  }

  if (token !== loadRecordToken) return

  try {
    await loadPrescriptions()
  } catch {
    ElMessage.warning('处方列表加载失败')
  }

  if (token !== loadRecordToken) return

  if (signed.value && !allExamsFinished.value) {
    startPolling()
  } else if (signed.value && allExamsFinished.value) {
    await tryRefineTreatmentPlan()
  }
}

function startPolling() {
  stopPolling()
  pollTimer.value = window.setInterval(async () => {
    try {
      await loadExamOrderList()
      await loadExamReports()
    } catch {
      // 轮询失败时保留已有列表
    }
    if (allExamsFinished.value) {
      stopPolling()
      await tryRefineTreatmentPlan()
    }
  }, 5000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = undefined
  }
}

function focusExamPanel() {
  if (signed.value) return
  aiMode.value = 'exam'
  fetchAiExams()
}

function focusRxPanel() {
  if (signed.value) return
  aiMode.value = 'rx'
  fetchAiRx()
  if (rxItems.value.length > 0 && !aiRxReview.value) {
    fetchAiRxReview()
  }
}

function toggleExamEditor() {
  if (signed.value) return
  showExamEditor.value = !showExamEditor.value
  if (showExamEditor.value) showRxEditor.value = false
}

function toggleRxEditor() {
  if (signed.value) return
  showRxEditor.value = !showRxEditor.value
  if (showRxEditor.value) showExamEditor.value = false
}

function examCatalogKey(item: ExamCatalogItem) {
  return `${item.orderType}-${item.itemId}`
}

function addPendingExam(item: ExamCatalogItem) {
  const key = examCatalogKey(item)
  if (pendingExams.value.some((e) => e.key === key)) return
  pendingExams.value = [...pendingExams.value, { ...item, key }]
}

function removePendingExam(key: string) {
  pendingExams.value = pendingExams.value.filter((e) => e.key !== key)
}

function onExamPickerChange(value: string) {
  if (!value) return
  const [orderType, itemIdStr] = value.split(':')
  const itemId = Number(itemIdStr)
  const catalog = orderType === 'LAB'
    ? labCatalog.value.find((i) => i.itemId === itemId)
    : examCatalog.value.find((i) => i.itemId === itemId)
  if (catalog) addPendingExam(catalog)
  examPicker.value = ''
}

function emptyRxRow(): PrescriptionItemRow {
  return {
    drugId: undefined,
    drugName: '',
    drugCode: '',
    dosage: 1,
    unit: '片',
    frequency: '每日1次',
    durationDays: 7,
  }
}

function addRxRow() {
  rxItems.value.push(emptyRxRow())
}

function removeRxRow(index: number) {
  if (rxItems.value.length <= 1) {
    rxItems.value = []
    return
  }
  rxItems.value.splice(index, 1)
}

function onRxDrugSelect(row: PrescriptionItemRow, drugId: number) {
  const drug = drugs.value.find((d) => d.id === drugId)
  if (!drug) return
  row.drugId = drug.id
  row.drugName = drug.drugName
  row.drugCode = drug.drugCode
  row.unit = drug.unit || row.unit
}

async function fetchAiRxReview() {
  if (!props.patientInfo || signed.value) return
  const items = rxItems.value.filter((i) => i.drugId || i.drugName)
  if (!items.length) {
    ElMessage.warning('请先添加药品后再审核')
    return
  }
  aiLoading.value = true
  try {
    aiRxReview.value = await reviewPrescriptionApi({
      patientId: props.patientInfo.patientId,
      items,
      diagnosis: form.diagnosis,
    })
    aiMode.value = 'rx'
  } catch {
    ElMessage.warning('AI+KG 处方审核失败')
  } finally {
    aiLoading.value = false
  }
}

async function resolveRxReviewForSign() {
  if (aiRxReview.value) {
    return aiRxReview.value
  }
  if (!props.patientInfo) return null
  const items = rxItems.value.filter((i) => i.drugId || i.drugName)
  try {
    const review = await reviewPrescriptionApi({
      patientId: props.patientInfo.patientId,
      items,
      diagnosis: form.diagnosis,
    })
    aiRxReview.value = review
    return review
  } catch {
    return null
  }
}

async function fetchAiMedicalRecord() {
  if (!props.registrationId || signed.value) return
  const dialogText = oralDialogText.value.trim()
  if (!dialogText) return

  medicalStreamAbort?.abort()
  medicalStreamAbort = new AbortController()
  recordStreaming.value = true
  medicalStreamText.value = ''
  recordStreamStatus.value = '准备生成…'
  aiMedicalDraft.value = null
  aiMode.value = 'record'

  let streamCompleted = false
  try {
    await generateMedicalRecordStreamApi(
      {
        registrationId: props.registrationId,
        dialogText,
      },
      {
        onDelta: (text) => {
          medicalStreamText.value += text
        },
        onStatus: (message) => {
          recordStreamStatus.value = message
        },
        onComplete: (payload) => {
          const draft = normalizeMedicalDraft(payload)
          if (!draft) {
            ElMessage.warning('AI 病历解析失败，请稍后重试')
            return
          }
          streamCompleted = true
          recordStreaming.value = false
          medicalStreamText.value = ''
          aiMedicalDraft.value = draft
          if (draft.recommendedExams?.length) {
            aiExamList.value = draft.recommendedExams
          }
          if (draft.kgAvailable) {
            ElMessage.success('病历已由 AI + 知识图谱融合生成')
          } else if (draft.aiAvailable) {
            ElMessage.success('AI 病历草稿已生成')
          } else {
            ElMessage.success('病历草稿已生成')
          }
        },
        onError: (message) => {
          if (streamCompleted) return
          ElMessage.warning(message || 'AI 病历生成失败，请稍后重试')
        },
      },
      medicalStreamAbort.signal,
    )
  } catch {
    if (!streamCompleted && !medicalStreamAbort?.signal.aborted) {
      ElMessage.warning('AI 病历生成失败，请稍后重试')
    }
  } finally {
    recordStreaming.value = false
    medicalStreamAbort = null
  }
}

async function fetchAiExams() {
  if (signed.value) return
  aiLoading.value = true
  try {
    const result = await recommendExamsApi({
      chiefComplaint: form.chiefComplaint,
      symptoms: form.presentIllness,
      diagnosis: form.diagnosis,
    })
    aiExamList.value = result.recommendedExams || []
    aiExamSummary.value = result.summary || ''
    aiExamKgAvailable.value = !!result.kgAvailable
    aiExamKgReference.value = result.kgReference || ''
  } catch {
    ElMessage.warning('AI 检查推荐失败')
  } finally {
    aiLoading.value = false
  }
}

async function fetchAiRx() {
  if (!props.patientInfo || signed.value) return
  aiLoading.value = true
  try {
    const result = await suggestPrescriptionApi({
      patientId: props.patientInfo.patientId,
      chiefComplaint: form.chiefComplaint,
      symptoms: form.presentIllness,
      diagnosis: form.diagnosis,
    })
    aiRxItems.value = result.recommendedItems || []
    aiRxRiskLevel.value = result.riskLevel
    aiRxAdvice.value = result.overallAdvice
    aiRxWarnings.value = result.warnings || []
  } catch {
    ElMessage.warning('AI 处方推荐失败')
  } finally {
    aiLoading.value = false
  }
}

function onAiRefresh() {
  if (aiMode.value === 'record') {
    fetchAiMedicalRecord()
  } else if (aiMode.value === 'exam') {
    fetchAiExams()
  } else {
    fetchAiRx()
  }
}

function applyAiRecord(draft: AiMedicalDraft) {
  if (draft.chiefComplaint) form.chiefComplaint = draft.chiefComplaint
  if (draft.presentIllness) form.presentIllness = draft.presentIllness
  if (draft.pastHistory) form.pastHistory = draft.pastHistory
  if (draft.allergyHistory) form.allergyHistory = draft.allergyHistory
  if (draft.personalHistory) form.personalHistory = draft.personalHistory
  if (draft.maritalHistory) form.maritalHistory = draft.maritalHistory
  if (draft.familyHistory) form.familyHistory = draft.familyHistory
  if (draft.temperature) form.temperature = draft.temperature
  if (draft.respiration) form.respiration = draft.respiration
  if (draft.pulse) form.pulse = draft.pulse
  if (draft.bloodPressure) form.bloodPressure = draft.bloodPressure
  if (draft.physicalExam) form.physicalExam = draft.physicalExam
  if (draft.auxiliaryExam) form.auxiliaryExam = draft.auxiliaryExam
  if (draft.diagnosis) form.diagnosis = draft.diagnosis
  if (draft.treatmentPlan) form.treatmentPlan = draft.treatmentPlan
  recordAiApplied.value = true
  ElMessage.success('AI 病历已填入，请检查后签名')
}

async function onSign() {
  if (!props.registrationId || !props.patientInfo) return
  if (!form.chiefComplaint.trim()) {
    ElMessage.warning('请填写主诉')
    return
  }
  if (pendingExams.value.length === 0 && rxItems.value.length === 0) {
    ElMessage.warning('请至少选择检查/检验或开药项目后再签名')
    return
  }

  await ElMessageBox.confirm(
    '签名后将正式开立检查/检验/处方，患者需缴费后执行。是否确认签名？',
    '医生签名确认',
    { type: 'warning' },
  )

  signing.value = true
  try {
    const examSnapshot = [...pendingExams.value]
    const physicalExam = buildPhysicalExamText()
    const record = await saveMedicalRecordApi({
      registrationId: props.registrationId,
      dialogText: oralDialogText.value.trim() || undefined,
      chiefComplaint: form.chiefComplaint,
      presentIllness: form.presentIllness,
      pastHistory: [form.pastHistory, form.allergyHistory ? `过敏史：${form.allergyHistory}` : '']
        .filter(Boolean)
        .join('\n'),
      physicalExam,
      diagnosis: form.diagnosis,
      treatmentPlan: form.treatmentPlan,
      status: 'DRAFT',
      isAiGenerated: recordAiApplied.value || aiMedicalDraft.value?.aiGenerated ? 1 : 0,
    })
    currentRecordId.value = record.id
    await confirmMedicalRecordApi(record.id)

    for (const exam of pendingExams.value) {
      await createExamOrderApi({
        registrationId: props.registrationId,
        orderType: exam.orderType,
        labItemId: exam.orderType === 'LAB' ? exam.itemId : undefined,
        examItemId: exam.orderType === 'EXAM' ? exam.itemId : undefined,
        clinicalIndication: form.diagnosis || form.chiefComplaint,
      })
    }

    if (rxItems.value.length > 0) {
      let riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' = 'LOW'
      let aiReviewResult: string | undefined
      const review = await resolveRxReviewForSign()
      if (review) {
        riskLevel = review.riskLevel
        aiReviewResult = review.overallAdvice
        if (review.riskLevel === 'HIGH') {
          await ElMessageBox.confirm(
            `AI+KG 融合审核为高风险：${review.overallAdvice}\n仍要开立处方吗？`,
            '处方风险提示',
            { type: 'warning' },
          )
        }
      }
      await createPrescriptionApi({
        registrationId: props.registrationId,
        items: rxItems.value.filter((i) => i.drugId || i.drugName),
        riskLevel,
        aiReviewResult,
      })
    }

    signed.value = true
    rememberSignedTreatmentPlan(props.registrationId, form.treatmentPlan)
    signedExamSnapshot.value = examSnapshot
    showExamEditor.value = false
    showRxEditor.value = false
    ElMessage.success('签名成功，检查/检验/处方已开立')
    try {
      await loadExamOrderList()
      await loadExamReports()
    } catch {
      ElMessage.warning('检查/检验列表刷新失败')
    }
    pendingExams.value = []
    try {
      await loadPrescriptions()
    } catch {
      ElMessage.warning('处方列表刷新失败')
    }
    if (!allExamsFinished.value) {
      startPolling()
    } else {
      await tryRefineTreatmentPlan()
    }
  } finally {
    signing.value = false
  }
}

async function onComplete() {
  if (!canComplete.value) {
    ElMessage.warning('请先完成签名并等待全部检查报告返回')
    return
  }
  await ElMessageBox.confirm('确认检查报告无误，结束本次看诊？', '结束看诊', { type: 'warning' })
  completing.value = true
  try {
    if (signed.value && form.treatmentPlan.trim()) {
      await persistTreatmentPlan(form.treatmentPlan)
    }
    emit('complete')
  } finally {
    completing.value = false
  }
}

async function onTreatmentPlanBlur() {
  if (!signed.value || refiningTreatmentPlan.value || !props.registrationId) return
  if (!form.treatmentPlan.trim()) return
  try {
    await persistTreatmentPlan(form.treatmentPlan)
  } catch {
    // 静默失败，结束看诊时会再次保存
  }
}

watch(
  () => props.registrationId,
  () => loadRecord(),
  { immediate: true },
)

watch(
  () => props.registration?.chiefComplaint,
  (val) => {
    if (val && !signed.value) {
      form.chiefComplaint = val
    }
  },
)

onUnmounted(() => {
  stopPolling()
  medicalStreamAbort?.abort()
  window.removeEventListener(DOCTOR_ALERT_EVENT, onDoctorAlert as EventListener)
})

function onDoctorAlert(event: Event) {
  const alert = (event as CustomEvent<DoctorAlertMessage>).detail
  if (!alert || alert.type !== 'PRESCRIPTION_HIGH_RISK') return
  if (signed.value) return
  aiMode.value = 'rx'
}

onMounted(() => {
  window.addEventListener(DOCTOR_ALERT_EVENT, onDoctorAlert as EventListener)
})

listDrugsApi().then((list) => {
  drugs.value = list
})
loadCatalog()
</script>

<template>
  <div class="emr-layout">
    <div class="emr-form">
    <div class="emr-title-bar">
      <h2>门诊病历</h2>
      <el-radio-group v-model="visitType" size="small" :disabled="signed">
        <el-radio-button value="initial">初诊</el-radio-button>
        <el-radio-button value="followup">复诊</el-radio-button>
      </el-radio-group>
    </div>

    <div class="patient-header" :class="{ collapsed: headerCollapsed }">
      <div class="avatar">患</div>
      <div class="header-fields">
        <div class="field-grid">
          <div><span class="lbl">门诊号</span>{{ patientInfo?.regNo || registration?.regNo || '-' }}</div>
          <div><span class="lbl">姓名</span>{{ patientInfo?.patientName || registration?.patientName }}</div>
          <div><span class="lbl">性别</span>{{ genderLabel(patientInfo?.gender) }}</div>
          <div><span class="lbl">科室</span>{{ registration?.deptName || '-' }}</div>
          <div><span class="lbl">排队</span>{{ patientInfo?.queueNo ?? registration?.queueNo }}号</div>
          <div><span class="lbl">缴费</span>
            <el-tag :type="registration?.paid ? 'success' : 'warning'" size="small">
              {{ registration?.paid ? '已缴' : '待缴' }}
            </el-tag>
          </div>
          <div class="span-2 allergy">
            <span class="lbl">过敏史</span>{{ patientInfo?.allergyHistory || '无' }}
          </div>
        </div>
      </div>
      <button type="button" class="collapse-btn" @click="headerCollapsed = !headerCollapsed">
        {{ headerCollapsed ? '展开' : '收起' }}
      </button>
    </div>

    <div class="emr-body">
      <div class="emr-section">
        <div class="section-label">主诉</div>
        <el-input
          v-model="form.chiefComplaint"
          type="textarea"
          :rows="2"
          :disabled="signed"
          placeholder="来自挂号单主诉"
          class="underline-input"
        />
        <p v-if="registration?.chiefComplaint" class="hint">挂号主诉：{{ registration.chiefComplaint }}</p>
      </div>

      <div class="emr-section">
        <div class="section-label">现病史</div>
        <el-input v-model="form.presentIllness" type="textarea" :rows="2" :disabled="signed" class="underline-input" />
      </div>

      <div class="emr-row-2">
        <div class="emr-section">
          <div class="section-label">既往史</div>
          <el-input v-model="form.pastHistory" type="textarea" :rows="2" :disabled="signed" class="underline-input" />
        </div>
        <div class="emr-section">
          <div class="section-label">过敏史</div>
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" :disabled="signed" class="underline-input" />
        </div>
      </div>

      <div class="emr-row-3">
        <div class="emr-section">
          <div class="section-label">个人史</div>
          <el-input v-model="form.personalHistory" :disabled="signed" class="underline-input" />
        </div>
        <div class="emr-section">
          <div class="section-label">婚育史</div>
          <el-input v-model="form.maritalHistory" :disabled="signed" class="underline-input" />
        </div>
        <div class="emr-section">
          <div class="section-label">家族史</div>
          <el-input v-model="form.familyHistory" :disabled="signed" class="underline-input" />
        </div>
      </div>

      <div class="emr-section vitals">
        <div class="section-label">查体</div>
        <div class="vitals-row">
          <span>体温</span>
          <el-input v-model="form.temperature" :disabled="signed" class="vital-input" /> ℃
          <span>呼吸</span>
          <el-input v-model="form.respiration" :disabled="signed" class="vital-input" /> 次/分
          <span>脉搏</span>
          <el-input v-model="form.pulse" :disabled="signed" class="vital-input" /> 次/分
          <span>血压</span>
          <el-input v-model="form.bloodPressure" :disabled="signed" class="vital-input" /> mmHg
        </div>
        <el-input
          v-model="form.physicalExam"
          type="textarea"
          :rows="2"
          :disabled="signed"
          placeholder="体格检查详情"
          class="underline-input mt-8"
        />
      </div>

      <div class="emr-section">
        <div class="section-label">辅助检查</div>
        <el-input
          v-model="form.auxiliaryExam"
          type="textarea"
          :rows="3"
          readonly
          placeholder="患者完成检查后，报告将自动回填至此"
          class="underline-input report-area"
        />
      </div>

      <div class="emr-section">
        <div class="section-label">
          处理意见
          <span v-if="refiningTreatmentPlan" class="plan-hint">AI 优化中…</span>
          <el-tag v-else-if="treatmentPlanRefined" type="success" size="small" class="plan-tag">已结合检查结果优化</el-tag>
          <el-button
            v-if="signed && allExamsFinished && !refiningTreatmentPlan"
            type="primary"
            link
            size="small"
            class="plan-refine-btn"
            @click="maybeRefineTreatmentPlan(true)"
          >
            {{ treatmentPlanRefined ? '重新 AI 优化' : 'AI 优化处理意见' }}
          </el-button>
        </div>
        <el-input
          v-model="form.treatmentPlan"
          type="textarea"
          :rows="3"
          :disabled="refiningTreatmentPlan"
          class="underline-input"
          :placeholder="signed ? '检查报告返回后 AI 将自动优化，您也可手动修改' : '填写处理意见'"
          @blur="onTreatmentPlanBlur"
        />
      </div>

      <div class="emr-section">
        <div class="section-label">诊断</div>
        <el-input v-model="form.diagnosis" :disabled="signed" class="underline-input" placeholder="1. 诊断名称" />
      </div>

      <div
        class="order-block clickable"
        :class="{ disabled: signed, active: aiMode === 'exam' && !signed }"
        @click="focusExamPanel"
      >
        <div class="order-head">
          <span class="order-title">| 检查 / 检验</span>
          <span class="order-count">共 {{ examCount }} 项</span>
          <span
            v-if="!signed"
            class="order-action"
            @click.stop="toggleExamEditor"
          >
            {{ showExamEditor ? '收起' : '查看全部' }}
          </span>
        </div>
        <template v-if="!signed && showExamEditor">
          <div class="order-editor" @click.stop>
            <div class="editor-toolbar">
              <el-select
                v-model="examPicker"
                placeholder="从目录选择检验/检查项目"
                filterable
                clearable
                size="small"
                style="width: 100%"
                @change="onExamPickerChange"
              >
                <el-option-group label="检验项目">
                  <el-option
                    v-for="item in labCatalog"
                    :key="`pick-lab-${item.itemId}`"
                    :label="`${item.examType}（${item.bodyPart || item.itemCode}）`"
                    :value="`LAB:${item.itemId}`"
                    :disabled="pendingExams.some((e) => e.key === examCatalogKey(item))"
                  />
                </el-option-group>
                <el-option-group label="检查项目">
                  <el-option
                    v-for="item in examCatalog"
                    :key="`pick-exam-${item.itemId}`"
                    :label="`${item.examType}（${item.bodyPart || item.itemCode}）`"
                    :value="`EXAM:${item.itemId}`"
                    :disabled="pendingExams.some((e) => e.key === examCatalogKey(item))"
                  />
                </el-option-group>
              </el-select>
            </div>
            <el-table v-if="pendingExams.length" :data="pendingExams" size="small" border>
              <el-table-column label="类型" width="70">
                <template #default="{ row }">
                  {{ orderTypeLabel(row.orderType) }}
                </template>
              </el-table-column>
              <el-table-column prop="examType" label="项目名称" />
              <el-table-column prop="bodyPart" label="部位/类别" width="100" />
              <el-table-column label="操作" width="70">
                <template #default="{ row }">
                  <el-button type="danger" link @click="removePendingExam(row.key)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="order-empty">暂未选择，请从上方目录添加</div>
          </div>
        </template>
        <template v-else>
          <el-table
            v-if="visibleExamRows.length"
            :data="visibleExamRows"
            :row-key="(row: DisplayExamRow) => String(row.id)"
            size="small"
            border
          >
            <el-table-column label="类型" width="70">
              <template #default="{ row }">
                {{ orderTypeLabel(row.orderType) }}
              </template>
            </el-table-column>
            <el-table-column label="项目名称" min-width="120">
              <template #default="{ row }">
                <el-button
                  v-if="!row.isPending && typeof row.id === 'number' && examReports[row.id]"
                  type="primary"
                  link
                  @click="openExamReport(row as ExamOrder)"
                >
                  {{ row.examType }}
                </el-button>
                <span v-else>{{ row.examType }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="bodyPart" label="部位/类别" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                {{ examStatusLabel[row.status] || row.status }}
              </template>
            </el-table-column>
            <el-table-column label="报告结论" min-width="160">
              <template #default="{ row }">
                <template v-if="row.isPending">
                  <span class="muted">签名后正式开立</span>
                </template>
                <template v-else-if="typeof row.id === 'number' && examReports[row.id]">
                  <span class="report-preview">{{ examReports[row.id].conclusion || '—' }}</span>
                  <el-button type="primary" link @click="openExamReport(row as ExamOrder)">查看详情</el-button>
                </template>
                <span v-else class="muted">{{ row.status === 'DONE' ? '报告生成中' : '待患者检查' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="order-empty">暂未选择检查/检验项目</div>
        </template>
      </div>

      <div
        class="order-block clickable"
        :class="{ disabled: signed, active: aiMode === 'rx' && !signed }"
        @click="focusRxPanel"
      >
        <div class="order-head">
          <span class="order-title">| 处方 Rp</span>
          <span class="order-count">共 {{ rxItems.length }} 项</span>
          <el-tag
            v-if="signed && latestPrescriptionStatus"
            :type="latestPrescriptionStatus === 'DISPENSED' ? 'success' : 'warning'"
            size="small"
            class="rx-status-tag"
          >
            {{ prescriptionStatusLabel[latestPrescriptionStatus] || latestPrescriptionStatus }}
          </el-tag>
          <span
            v-if="!signed"
            class="order-action"
            @click.stop="toggleRxEditor"
          >
            {{ showRxEditor ? '收起' : '查看全部' }}
          </span>
        </div>
        <template v-if="showRxEditor && !signed">
          <div class="order-editor" @click.stop>
            <div class="editor-toolbar">
              <el-button size="small" @click="addRxRow">添加药品</el-button>
            </div>
            <el-table v-if="rxItems.length" :data="rxItems" border size="small">
              <el-table-column label="药品" min-width="180">
                <template #default="{ row }">
                  <el-select
                    :model-value="row.drugId"
                    placeholder="选择药品"
                    filterable
                    size="small"
                    style="width: 100%"
                    @change="(val: number) => onRxDrugSelect(row, val)"
                  >
                    <el-option
                      v-for="drug in drugs"
                      :key="drug.id"
                      :label="`${drug.drugName}（${drug.specification || ''}）`"
                      :value="drug.id"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="剂量" width="90">
                <template #default="{ row }">
                  <el-input-number v-model="row.dosage" :min="0.5" :step="0.5" controls-position="right" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="单位" width="70">
                <template #default="{ row }">
                  <el-input v-model="row.unit" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="频次" width="100">
                <template #default="{ row }">
                  <el-input v-model="row.frequency" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="天数" width="80">
                <template #default="{ row }">
                  <el-input-number v-model="row.durationDays" :min="1" controls-position="right" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60">
                <template #default="{ $index }">
                  <el-button type="danger" link @click="removeRxRow($index)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="order-empty">暂未选择，点击「添加药品」手动录入</div>
          </div>
        </template>
        <template v-else>
          <el-table v-if="rxItems.length" :data="rxItems" size="small" border>
            <el-table-column prop="drugName" label="药品名称" />
            <el-table-column prop="dosage" label="剂量" width="70" />
            <el-table-column prop="unit" label="单位" width="60" />
            <el-table-column prop="frequency" label="频次" width="100" />
            <el-table-column prop="durationDays" label="天数" width="70" />
          </el-table>
          <div v-else class="order-empty">暂未选择药品</div>
        </template>
      </div>
    </div>

    <footer class="emr-footer">
      <div class="footer-left">
        <template v-if="signed">
          <el-tag type="success">已签名</el-tag>
          <span class="sign-info">签名医师：{{ userStore.userInfo?.realName }}</span>
          <span v-if="!allExamsFinished" class="wait-hint">等待检查报告返回中（自动刷新）…</span>
          <span v-else-if="refiningTreatmentPlan" class="wait-hint">检查报告已返回，AI 正在优化处理意见…</span>
          <span v-else class="ready-hint">全部报告已返回，可结束看诊</span>
        </template>
        <span v-else class="sign-hint">在右侧 AI 助手录入患者口述，解析后预览并手动填入病历</span>
      </div>
      <div class="footer-right">
        <el-button
          v-if="!signed"
          type="primary"
          :loading="signing"
          @click="onSign"
        >
          医生签名
        </el-button>
        <el-button
          v-else
          type="danger"
          :loading="completing"
          :disabled="!canComplete"
          @click="onComplete"
        >
          结束看诊
        </el-button>
      </div>
    </footer>
    </div>

    <DoctorAiSidebar
      v-model:mode="aiMode"
      v-model:pending-exams="pendingExams"
      v-model:rx-items="rxItems"
      v-model:dialog-text="oralDialogText"
      :loading="aiLoading"
      :signed="signed"
      :record-streaming="recordStreaming"
      :medical-stream-text="medicalStreamText"
      :stream-status="recordStreamStatus"
      :medical-draft="aiMedicalDraft"
      :exam-list="aiExamList"
      :exam-summary="aiExamSummary"
      :exam-kg-available="aiExamKgAvailable"
      :exam-kg-reference="aiExamKgReference"
      :lab-catalog="labCatalog"
      :exam-catalog="examCatalog"
      :drugs="drugs"
      :ai-rx-items="aiRxItems"
      :rx-risk-level="aiRxRiskLevel"
      :rx-advice="aiRxAdvice"
      :rx-warnings="aiRxWarnings"
      :rx-review="aiRxReview"
      :clinical-indication="form.diagnosis || form.chiefComplaint"
      @refresh="onAiRefresh"
      @generate-record="fetchAiMedicalRecord"
      @apply-record="applyAiRecord"
      @review-rx="fetchAiRxReview"
    />

    <ExamReportDetailDialog
      v-model="examReportDialogVisible"
      :order="viewingExamOrder"
      :report="viewingExamReport"
    />
  </div>
</template>

<style scoped>
.emr-layout {
  display: flex;
  height: 100%;
  min-width: 0;
}

.emr-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

.emr-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.emr-title-bar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.patient-header {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafbfc;
  flex-shrink: 0;
  position: relative;
}

.patient-header.collapsed .header-fields {
  display: none;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px 24px;
  font-size: 14px;
}

.field-grid .lbl {
  color: #909399;
  margin-right: 8px;
}

.field-grid .span-2 {
  grid-column: span 2;
}

.field-grid .allergy {
  color: #f56c6c;
}

.collapse-btn {
  position: absolute;
  right: 16px;
  bottom: 8px;
  border: none;
  background: none;
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
}

.emr-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 80px;
}

.emr-section {
  margin-bottom: 16px;
}

.section-label {
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}

.emr-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.emr-row-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.vitals-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.vital-input {
  width: 72px;
}

.mt-8 {
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin: 4px 0 0;
}

.underline-input :deep(.el-textarea__inner),
.underline-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 0;
  border-bottom: 1px solid #dcdfe6;
  padding-left: 0;
}

.report-area :deep(.el-textarea__inner) {
  background: #f5f7fa;
}

.order-block {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 16px;
}

.order-block.clickable {
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.order-block.clickable:hover:not(.disabled) {
  border-color: #409eff;
  background: #f5f9ff;
}

.order-block.disabled {
  cursor: default;
}

.order-block.active {
  border-color: #667eea;
  background: #f8f5ff;
  box-shadow: inset 3px 0 0 #667eea;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.order-title {
  font-weight: 600;
  color: #303133;
}

.order-count {
  color: #909399;
  font-size: 13px;
}

.order-action {
  margin-left: auto;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
}

.order-action:hover {
  text-decoration: underline;
}

.order-editor {
  margin-top: 4px;
}

.editor-toolbar {
  margin-bottom: 10px;
}

.order-empty {
  color: #c0c4cc;
  font-size: 13px;
  padding: 8px 0;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.report-preview {
  display: inline-block;
  max-width: 180px;
  margin-right: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #606266;
  font-size: 12px;
}

.plan-hint {
  margin-left: 8px;
  color: #409eff;
  font-size: 12px;
  font-weight: normal;
}

.plan-tag {
  margin-left: 8px;
  vertical-align: middle;
}

.plan-refine-btn {
  margin-left: 8px;
  vertical-align: middle;
}

.emr-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.sign-info {
  color: #303133;
}

.wait-hint {
  color: #e6a23c;
}

.ready-hint {
  color: #67c23a;
}

.sign-hint {
  color: #909399;
}

.mb-12 {
  margin-bottom: 12px;
}
</style>
