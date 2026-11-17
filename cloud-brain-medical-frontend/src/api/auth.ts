import { post } from '@/utils/request'
import type { LoginResponse } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export function loginApi(data: LoginParams) {
  return post<LoginResponse>('/auth/login', data)
}
