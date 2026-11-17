import { del, get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export type TimeSlot = 'MORNING' | 'AFTERNOON' | 'EVENING'

export interface AdminSchedule {
  id: number
  doctorId: number
  doctorName: string
  doctorTitle?: string
  departmentId?: number
  departmentName?: string
  doctorAvailable?: boolean
  workDate: string
  timeSlot: TimeSlot
  maxPatients: number
  registeredCount: number
  remainingSlots: number
  slotStatus?: string
  dutyStatus?: string
  isAiGenerated?: number
}

export interface CreateScheduleParams {
  doctorId: number
  workDate: string
  timeSlot: TimeSlot
  maxPatients: number
}

export interface UpdateScheduleParams {
  doctorId?: number
  workDate?: string
  timeSlot?: TimeSlot
  maxPatients: number
}

export interface ScheduleQueryParams extends PageQuery {
  departmentId?: number
  workDate?: string
  timeSlot?: TimeSlot
}

export function adminSchedulesApi(params?: ScheduleQueryParams) {
  return get<PageResult<AdminSchedule>>('/admin/schedules', { params })
}

export function adminSchedulesAllApi(params?: Omit<ScheduleQueryParams, 'page' | 'size'>) {
  return get<AdminSchedule[]>('/admin/schedules/all', { params })
}

export function createAdminScheduleApi(data: CreateScheduleParams) {
  return post<AdminSchedule>('/admin/schedules', data)
}

export function updateAdminScheduleApi(id: number, data: UpdateScheduleParams) {
  return put<AdminSchedule>(`/admin/schedules/${id}`, data)
}

export function deleteAdminScheduleApi(id: number) {
  return del<void>(`/admin/schedules/${id}`)
}

export interface BatchPublishScheduleResult {
  successCount: number
  skippedCount: number
  errors: string[]
}

export function batchPublishAdminSchedulesApi(items: CreateScheduleParams[]) {
  return post<BatchPublishScheduleResult>('/admin/schedules/batch', { items })
}
