import { get, post } from '@/utils/request'
import { postSse, type PostSseHandlers } from '@/utils/sseClient'
import type {
  AiMedicalDraft,
  ExamRecommendResult,
  PrescriptionItem,
  PrescriptionReviewResult,
  PrescriptionSuggestResult,
} from '@/types/api'

export interface ReviewPrescriptionParams {
  patientId: number
  items: PrescriptionItem[]
  diagnosis?: string
}

export function reviewPrescriptionApi(data: ReviewPrescriptionParams) {
  return post<PrescriptionReviewResult>('/ai/prescription/review', data)
}

export interface GenerateMedicalRecordParams {
  registrationId: number
  dialogText: string
}

export function generateMedicalRecordApi(data: GenerateMedicalRecordParams) {
  return post<AiMedicalDraft>('/ai/medical-record/generate', data)
}

export function generateMedicalRecordStreamApi(
  data: GenerateMedicalRecordParams,
  handlers: PostSseHandlers,
  signal?: AbortSignal,
) {
  return postSse('/ai/medical-record/generate/stream', data, handlers, signal)
}

export interface SpeechTranscribeResult {
  text: string
  speechAvailable: boolean
}

export function getSpeechStatusApi() {
  return get<SpeechTranscribeResult>('/ai/speech/status')
}

export function transcribeSpeechApi(file: Blob) {
  const formData = new FormData()
  formData.append('file', file, 'recording.wav')
  return post<SpeechTranscribeResult>('/ai/speech/transcribe', formData, {
    timeout: 120000,
  })
}

export interface RefineTreatmentPlanParams {
  registrationId: number
  treatmentPlan?: string
  diagnosis?: string
}

export interface TreatmentPlanRefineResult {
  treatmentPlan: string
  aiAvailable: boolean
}

export function refineTreatmentPlanApi(data: RefineTreatmentPlanParams) {
  return post<TreatmentPlanRefineResult>('/ai/medical-record/refine-treatment-plan', data, {
    skipErrorToast: true,
  })
}

export interface ExamRecommendParams {
  chiefComplaint?: string
  symptoms?: string
  diagnosis?: string
}

export function recommendExamsApi(data: ExamRecommendParams) {
  return post<ExamRecommendResult>('/ai/exam/recommend', data)
}

export interface PrescriptionSuggestParams {
  patientId: number
  chiefComplaint?: string
  symptoms?: string
  diagnosis?: string
}

export function suggestPrescriptionApi(data: PrescriptionSuggestParams) {
  return post<PrescriptionSuggestResult>('/ai/prescription/suggest', data)
}

export function pingAiApi(message?: string) {
  return get<{ available: boolean; reply?: string; error?: string }>('/ai/ping', {
    params: message ? { message } : undefined,
  })
}

export type AiTimeSlot = 'MORNING' | 'AFTERNOON' | 'EVENING'

export interface AiScheduleSuggestion {
  doctorId: number
  doctorName: string
  workDate: string
  timeSlot: AiTimeSlot
  maxPatients: number
  reason?: string
}

export interface GenerateScheduleParams {
  deptId: number
  startDate: string
  endDate: string
}

export interface AiScheduleGenerateResult {
  schedules: AiScheduleSuggestion[]
  aiAvailable: boolean
  summary?: string
}

export function generateAiScheduleApi(data: GenerateScheduleParams) {
  return post<AiScheduleGenerateResult>('/ai/schedule/generate', data)
}
