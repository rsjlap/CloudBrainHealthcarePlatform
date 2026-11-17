import { get } from '@/utils/request'
import type { Department } from '@/types/api'

export function listDepartmentsApi() {
  return get<Department[]>('/departments')
}
