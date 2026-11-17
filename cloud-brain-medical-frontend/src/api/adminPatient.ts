import { get, put } from '@/utils/request'
import type { MedicalRecord, PageQuery, PageResult, Registration } from '@/types/api'

export interface AdminPatientStats {
  totalCount: number
  maleCount: number
  femaleCount: number
  allergyCount: number
  todayNewCount: number
  visitedCount: number
  activeCount: number
}

export interface AdminPatientListItem {
  id: number
  username: string
  realName: string
  phone: string
  gender?: number
  genderLabel?: string
  allergyHistory?: string
  status: number
  registrationCount: number
  medicalRecordCount: number
  createdAt?: string
}

export interface AdminPatientDetail {
  id: number
  username: string
  realName: string
  phone: string
  status: number
  gender?: number
  genderLabel?: string
  birthDate?: string
  idCard?: string
  bloodType?: string
  allergyHistory?: string
  medicalHistory?: string
  emergencyContact?: string
  emergencyPhone?: string
  createdAt?: string
  registrations: Registration[]
  medicalRecords: MedicalRecord[]
}

export interface AdminPatientQuery extends PageQuery {
  keyword?: string
  gender?: number
  hasAllergy?: boolean
  registerDateFrom?: string
  registerDateTo?: string
}

export interface UpdateAdminPatientPayload {
  realName?: string
  phone?: string
  gender?: number
  birthDate?: string
  idCard?: string
  bloodType?: string
  allergyHistory?: string
  medicalHistory?: string
  emergencyContact?: string
  emergencyPhone?: string
  status?: number
}

export function adminPatientStatsApi() {
  return get<AdminPatientStats>('/admin/patients/stats')
}

export function adminPatientListApi(params?: AdminPatientQuery) {
  return get<PageResult<AdminPatientListItem>>('/admin/patients', { params })
}

export function adminPatientDetailApi(id: number) {
  return get<AdminPatientDetail>(`/admin/patients/${id}`)
}

export function updateAdminPatientApi(id: number, data: UpdateAdminPatientPayload) {
  return put<AdminPatientDetail>(`/admin/patients/${id}`, data)
}
