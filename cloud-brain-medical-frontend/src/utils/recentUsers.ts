import type { UserRole } from '@/types/api'

const RECENT_USERS_KEY = 'cbm_recent_users'
const MAX_RECENT_USERS = 8

export interface RecentUser {
  username: string
  realName?: string
  role?: UserRole
  lastLoginAt: number
}

export function loadRecentUsers(): RecentUser[] {
  try {
    const raw = localStorage.getItem(RECENT_USERS_KEY)
    if (!raw) return []
    const list = JSON.parse(raw) as RecentUser[]
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

export function saveRecentUser(user: Omit<RecentUser, 'lastLoginAt'>) {
  const list = loadRecentUsers().filter((item) => item.username !== user.username)
  list.unshift({
    ...user,
    lastLoginAt: Date.now(),
  })
  localStorage.setItem(RECENT_USERS_KEY, JSON.stringify(list.slice(0, MAX_RECENT_USERS)))
}

export function removeRecentUser(username: string) {
  const list = loadRecentUsers().filter((item) => item.username !== username)
  localStorage.setItem(RECENT_USERS_KEY, JSON.stringify(list))
}

export function getLastRecentUsername(): string {
  const list = loadRecentUsers()
  return list[0]?.username ?? ''
}

export const roleLabels: Record<UserRole, string> = {
  ADMIN: '管理员',
  DOCTOR: '医生',
  PATIENT: '患者',
  PHARMACIST: '药师',
  INSPECTOR: '检查医师',
}
