import { get, post, put } from '@/utils/request'

export type ExamOrderStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE'
export type ExamOrderType = 'LAB' | 'EXAM'

export interface ExamOrder {
  id: number
  registrationId: number
  patientName?: string
  orderType?: ExamOrderType
  labItemId?: number
  examItemId?: number
  examType: string
  bodyPart?: string
  clinicalIndication?: string
  fee?: number
  status: ExamOrderStatus
  orderedByName?: string
  paid?: boolean
  createdAt?: string
}

export interface ExamReport {
  id: number
  orderId: number
  examType?: string
  reportText?: string
  conclusion?: string
  isAiGenerated?: number
  inspectorName?: string
  confirmedAt?: string
  createdAt?: string
}

export interface ExamReportDraft {
  reportText: string
  conclusion: string
  aiAvailable: boolean
}

export interface CreateExamOrderParams {
  registrationId: number
  orderType?: ExamOrderType
  labItemId?: number
  examItemId?: number
  examType?: string
  bodyPart?: string
  clinicalIndication?: string
}

export interface SaveExamReportParams {
  orderId: number
  reportText: string
  conclusion: string
  isAiGenerated?: number
}

export function createExamOrderApi(data: CreateExamOrderParams) {
  return post<ExamOrder>('/exam-orders', data)
}

export function listExamOrdersByRegistrationApi(registrationId: number) {
  return get<ExamOrder[]>(`/exam-orders/registrations/${registrationId}`)
}

export function pendingExamOrdersApi() {
  return get<ExamOrder[]>('/exam-orders/pending')
}

export function generateAndSaveExamReportApi(orderId: number) {
  return post<ExamReport>(`/exam-reports/generate/${orderId}`)
}

export function generateExamReportApi(data: {
  examType: string
  bodyPart?: string
  clinicalIndication?: string
}) {
  return post<ExamReportDraft>('/ai/exam-report/generate', data)
}

export function saveExamReportApi(data: SaveExamReportParams) {
  return post<ExamReport>('/exam-reports', data)
}

export function confirmExamReportApi(id: number) {
  return put<ExamReport>(`/exam-reports/${id}/confirm`)
}

export function getExamReportByOrderApi(orderId: number) {
  return get<ExamReport | null>(`/exam-reports/orders/${orderId}`)
}

export interface InspectorExamHistory {
  orderId: number
  reportId: number
  registrationId: number
  patientName?: string
  doctorName?: string
  orderType?: ExamOrderType
  examType?: string
  bodyPart?: string
  clinicalIndication?: string
  reportText?: string
  conclusion?: string
  inspectorName?: string
  isAiGenerated?: number
  orderCreatedAt?: string
  confirmedAt?: string
}

export function inspectorExamHistoryApi(params?: {
  patientName?: string
  doctorName?: string
  date?: string
}) {
  return get<InspectorExamHistory[]>('/exam-reports/inspector/history', params ? { params } : undefined)
}
