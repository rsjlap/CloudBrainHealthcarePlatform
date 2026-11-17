import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

declare module 'axios' {
  interface AxiosRequestConfig {
    skipErrorToast?: boolean
  }
}

const TOKEN_KEY = 'cbm_token'
const USER_KEY = 'cbm_user'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code === 200) {
      return result.data as never
    }
    if (result.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!window.location.pathname.includes('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
      }
    }
    if (!response.config.skipErrorToast) {
      ElMessage.error(result.message || '请求失败')
    }
    return Promise.reject(new Error(result.message || '请求失败'))
  },
  (error) => {
    if (!error.config?.skipErrorToast) {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

export function get<T>(url: string, config?: AxiosRequestConfig) {
  return service.get<T, T>(url, config)
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return service.post<T, T>(url, data, config)
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
  return service.put<T, T>(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig) {
  return service.delete<T, T>(url, config)
}

export default service
