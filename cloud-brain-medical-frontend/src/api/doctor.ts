import { get, put } from '@/utils/request'
import type { DoctorCard, DoctorProfile, Schedule } from '@/types/api'

export function listDoctorsApi(deptId: number) {
  return get<DoctorCard[]>('/doctors', { params: { deptId } })
}

export function getDoctorApi(id: number) {
  return get<DoctorCard>(`/doctors/${id}`)
}

export function getDoctorProfileApi() {
  return get<DoctorProfile>('/doctors/me/profile')
}

export interface UpdateDoctorProfileParams {
  specialty?: string
  introduction?: string
}

export function updateDoctorProfileApi(data: UpdateDoctorProfileParams) {
  return put<DoctorProfile>('/doctors/me/profile', data)
}

export function doctorSchedulesApi(startDate: string, endDate: string) {
  return get<Schedule[]>('/doctors/me/schedules', { params: { startDate, endDate } })
}
