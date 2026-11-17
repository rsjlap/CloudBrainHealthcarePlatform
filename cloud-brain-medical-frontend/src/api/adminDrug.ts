import { get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface AdminDrug {
  id: number
  drugCode: string
  drugName: string
  specification?: string
  unit?: string
  unitPrice?: number
  manufacturer?: string
  contraindication?: string
  status: number
  stockQuantity: number
}

export interface AdminDrugQuery extends PageQuery {
  keyword?: string
  status?: number
}

export interface SaveAdminDrugParams {
  drugCode: string
  drugName: string
  specification?: string
  unit?: string
  unitPrice?: number
  manufacturer?: string
  contraindication?: string
  status?: number
  stockQuantity?: number
}

export type UpdateAdminDrugParams = Partial<SaveAdminDrugParams>

export function adminDrugsApi(params?: AdminDrugQuery) {
  return get<PageResult<AdminDrug>>('/admin/drugs', { params })
}

export function adminDrugDetailApi(id: number) {
  return get<AdminDrug>(`/admin/drugs/${id}`)
}

export function createAdminDrugApi(data: SaveAdminDrugParams) {
  return post<AdminDrug>('/admin/drugs', data)
}

export function updateAdminDrugApi(id: number, data: UpdateAdminDrugParams) {
  return put<AdminDrug>(`/admin/drugs/${id}`, data)
}
