import { get } from '@/utils/request'
import type { PageQuery, PageResult, Registration, RegistrationStatus } from '@/types/api'

export interface AdminRegistrationQuery extends PageQuery {
  keyword?: string
  status?: RegistrationStatus
  departmentId?: number
  doctorId?: number
  paid?: boolean
  dateFrom?: string
  dateTo?: string
}

export function adminRegistrationsApi(params?: AdminRegistrationQuery) {
  return get<PageResult<Registration>>('/admin/registrations', { params })
}

export function adminRegistrationDetailApi(id: number) {
  return get<Registration>(`/admin/registrations/${id}`)
}
