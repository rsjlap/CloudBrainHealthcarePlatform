import { get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface AdminDoctor {
  id: number
  username: string
  realName: string
  phone: string
  status: number
  departmentId?: number
  departmentName?: string
  title?: string
  specialty?: string
  introduction?: string
  registrationFee?: number
  isAvailable?: number
  createdAt?: string
}

export interface AdminDoctorQuery extends PageQuery {
  keyword?: string
  departmentId?: number
  status?: number
}

export interface SaveAdminDoctorParams {
  username: string
  password: string
  realName: string
  phone?: string
  departmentId: number
  title?: string
  specialty?: string
  introduction?: string
  registrationFee?: number
}

export interface UpdateAdminDoctorParams {
  realName?: string
  phone?: string
  password?: string
  departmentId?: number
  title?: string
  specialty?: string
  introduction?: string
  registrationFee?: number
  isAvailable?: number
  status?: number
}

export function adminDoctorsApi(params?: AdminDoctorQuery) {
  return get<PageResult<AdminDoctor>>('/admin/doctors', { params })
}

export function adminDoctorDetailApi(id: number) {
  return get<AdminDoctor>(`/admin/doctors/${id}`)
}

export function createAdminDoctorApi(data: SaveAdminDoctorParams) {
  return post<AdminDoctor>('/admin/doctors', data)
}

export function updateAdminDoctorApi(id: number, data: UpdateAdminDoctorParams) {
  return put<AdminDoctor>(`/admin/doctors/${id}`, data)
}
