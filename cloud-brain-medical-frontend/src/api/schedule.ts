import { get } from '@/utils/request'
import type { Schedule } from '@/types/api'

export function listSchedulesApi(doctorId: number, date: string) {
  return get<Schedule[]>('/schedules', { params: { doctorId, date } })
}
