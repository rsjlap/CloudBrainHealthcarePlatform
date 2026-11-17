import { get, put } from '@/utils/request'
import type { PageQuery, PageResult, UserRole } from '@/types/api'

export interface AdminDashboard {
  todayRegistrations: number
  totalRegistrations: number
  inProgressCount: number
  completedCount: number
  patientCount: number
  doctorCount: number
  registrationTrend?: ChartCountItem[]
  departmentDistribution?: ChartCountItem[]
}

export interface ChartCountItem {
  label: string
  count: number
}

export interface AdminUser {
  id: number
  username: string
  realName: string
  phone: string
  role: UserRole
  status: number
  createdAt?: string
}

export function adminDashboardApi() {
  return get<AdminDashboard>('/admin/dashboard')
}

export function adminUsersApi() {
  return get<AdminUser[]>('/admin/users')
}

export function adminStaffApi(params?: { role?: UserRole; keyword?: string } & PageQuery) {
  return get<PageResult<AdminUser>>('/admin/users/staff', { params })
}

export interface UpdateAdminStaffParams {
  realName?: string
  phone?: string
  password?: string
  status?: number
}

export function updateAdminStaffApi(id: number, data: UpdateAdminStaffParams) {
  return put<AdminUser>(`/admin/users/staff/${id}`, data)
}
