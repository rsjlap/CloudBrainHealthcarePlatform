import { get } from '@/utils/request'

export interface LabItemCatalog {
  id: number
  itemCode: string
  itemName: string
  category: string
  description?: string
  departmentId?: number
  departmentName?: string
  fee: number
}

export interface ExamItemCatalog {
  id: number
  itemCode: string
  itemName: string
  bodyPart?: string
  description?: string
  departmentId?: number
  departmentName?: string
  equipmentType?: string
  fee: number
}

export function listLabItemsApi(keyword?: string) {
  return get<LabItemCatalog[]>('/catalog/lab-items', keyword ? { params: { keyword } } : undefined)
}

export function listExamItemsApi(keyword?: string) {
  return get<ExamItemCatalog[]>('/catalog/exam-items', keyword ? { params: { keyword } } : undefined)
}
