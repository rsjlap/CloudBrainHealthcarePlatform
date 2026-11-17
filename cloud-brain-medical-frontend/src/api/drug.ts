import { get } from '@/utils/request'
import type { Drug } from '@/types/api'

export function listDrugsApi() {
  return get<Drug[]>('/drugs')
}
