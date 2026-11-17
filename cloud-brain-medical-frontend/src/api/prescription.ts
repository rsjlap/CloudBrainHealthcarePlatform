import { get, post, put } from '@/utils/request'
import type { Prescription, PrescriptionItem, RiskLevel } from '@/types/api'

export interface CreatePrescriptionParams {
  registrationId: number
  items: PrescriptionItem[]
  riskLevel?: RiskLevel
  aiReviewResult?: string
}

export function createPrescriptionApi(data: CreatePrescriptionParams) {
  return post<Prescription>('/prescriptions', data)
}

export function myPrescriptionsApi() {
  return get<Prescription[]>('/prescriptions/my')
}

export function listPrescriptionsByRegistrationApi(registrationId: number) {
  return get<Prescription[]>(`/prescriptions/registrations/${registrationId}`)
}

export function pendingPrescriptionsApi() {
  return get<Prescription[]>('/prescriptions/pending')
}

export function getPrescriptionApi(id: number) {
  return get<Prescription>(`/prescriptions/${id}`)
}

export function dispensedPrescriptionsApi(params?: {
  patientName?: string
  doctorName?: string
  date?: string
}) {
  return get<Prescription[]>('/prescriptions/pharmacy/history', params ? { params } : undefined)
}

export function dispensePrescriptionApi(id: number) {
  return put<Prescription>(`/prescriptions/${id}/dispense`)
}
