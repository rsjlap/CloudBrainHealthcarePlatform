import { del, get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export type EquipmentStatus = 'ACTIVE' | 'DISABLED' | 'MAINTENANCE' | 'FAULT'

export interface EquipmentStats {
  totalCount: number
  activeCount: number
  disabledCount: number
  maintenanceCount: number
  faultCount: number
  abnormalCount: number
  statusDistribution: { status: EquipmentStatus; label: string; count: number }[]
}

export interface MedicalEquipment {
  id: number
  equipmentCode: string
  equipmentName: string
  category: string
  departmentId?: number
  departmentName?: string
  model?: string
  manufacturer?: string
  location?: string
  status: EquipmentStatus
  statusLabel?: string
  purchaseDate?: string
  lastMaintenanceDate?: string
  remark?: string
  updatedAt?: string
}

export interface EquipmentUsageRecord {
  id: number
  equipmentId: number
  equipmentName?: string
  usageType: string
  operatorName?: string
  patientInfo?: string
  startTime: string
  endTime?: string
  remark?: string
  createdAt?: string
}

export interface SaveEquipmentParams {
  equipmentCode: string
  equipmentName: string
  category: string
  departmentId?: number
  model?: string
  manufacturer?: string
  location?: string
  purchaseDate?: string
  lastMaintenanceDate?: string
  remark?: string
}

export interface CreateUsageParams {
  usageType: string
  operatorName?: string
  patientInfo?: string
  startTime: string
  endTime?: string
  remark?: string
}

export function equipmentStatsApi() {
  return get<EquipmentStats>('/admin/equipment/stats')
}

export function equipmentCategoryNamesApi() {
  return get<string[]>('/admin/equipment/category-names')
}

export function medicalEquipmentListApi(params?: {
  category?: string
  status?: EquipmentStatus
  keyword?: string
} & PageQuery) {
  return get<PageResult<MedicalEquipment>>('/admin/equipment', { params })
}

export function createMedicalEquipmentApi(data: SaveEquipmentParams) {
  return post<MedicalEquipment>('/admin/equipment', data)
}

export function updateMedicalEquipmentApi(id: number, data: SaveEquipmentParams) {
  return put<MedicalEquipment>(`/admin/equipment/${id}`, data)
}

export function updateEquipmentStatusApi(id: number, status: EquipmentStatus) {
  return put<MedicalEquipment>(`/admin/equipment/${id}/status`, { status })
}

export function deleteMedicalEquipmentApi(id: number) {
  return del<void>(`/admin/equipment/${id}`)
}

export function equipmentUsageRecordsApi(equipmentId: number) {
  return get<EquipmentUsageRecord[]>(`/admin/equipment/${equipmentId}/usage-records`)
}

export function createEquipmentUsageApi(equipmentId: number, data: CreateUsageParams) {
  return post<EquipmentUsageRecord>(`/admin/equipment/${equipmentId}/usage-records`, data)
}
