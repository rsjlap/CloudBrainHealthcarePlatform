import { get, post, put } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { MedicalRecord } from '@/types/api'

export interface SaveMedicalRecordParams {
  registrationId: number
  dialogText?: string
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  physicalExam?: string
  diagnosis?: string
  treatmentPlan?: string
  status?: 'DRAFT' | 'CONFIRMED'
  isAiGenerated?: number
  aiDraft?: string
}

export function saveMedicalRecordApi(data: SaveMedicalRecordParams, config?: AxiosRequestConfig) {
  return post<MedicalRecord>('/medical-records', data, config)
}

export function confirmMedicalRecordApi(id: number) {
  return put<MedicalRecord>(`/medical-records/${id}/confirm`)
}

export function getMedicalRecordApi(id: number) {
  return get<MedicalRecord>(`/medical-records/${id}`)
}

export function getMedicalRecordByRegistrationApi(registrationId: number) {
  return get<MedicalRecord | null>(`/medical-records/by-registration/${registrationId}`)
}

export function doctorMedicalRecordsApi(params?: { patientName?: string; date?: string }) {
  return get<MedicalRecord[]>('/medical-records/doctor/my', params ? { params } : undefined)
}
