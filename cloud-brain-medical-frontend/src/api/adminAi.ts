import { post } from '@/utils/request'

export interface AdminAiChatResult {
  answer: string
  queryType?: string
  sources?: string[]
  aiAvailable: boolean
}

export function adminAiChatApi(question: string) {
  return post<AdminAiChatResult>('/admin/ai/chat', { question })
}
