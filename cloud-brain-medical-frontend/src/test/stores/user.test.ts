import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import type { LoginResponse, UserInfo } from '@/types/api'

vi.mock('@/utils/request', () => ({
  get: vi.fn(),
}))

import { get } from '@/utils/request'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.mocked(get).mockReset()
  })

  const loginData: LoginResponse = {
    token: 'jwt-token',
    tokenType: 'Bearer',
    expiresIn: 86400000,
    userId: 2,
    username: 'doctor1',
    realName: '张医生',
    role: 'DOCTOR',
  }

  it('正常：登录后写入 token 与用户信息', () => {
    const store = useUserStore()

    store.setLogin(loginData)

    expect(store.isLoggedIn).toBe(true)
    expect(store.isDoctor).toBe(true)
    expect(store.isAdmin).toBe(false)
    expect(store.token).toBe('jwt-token')
    expect(store.userInfo?.realName).toBe('张医生')
    expect(localStorage.getItem('cbm_token')).toBe('jwt-token')
    expect(JSON.parse(localStorage.getItem('cbm_user')!)).toMatchObject({
      username: 'doctor1',
      role: 'DOCTOR',
    })
  })

  it('正常：从 localStorage 恢复登录态', () => {
    localStorage.setItem('cbm_token', 'saved-token')
    localStorage.setItem('cbm_user', JSON.stringify({
      userId: 1,
      username: 'admin',
      role: 'ADMIN',
    } satisfies Partial<UserInfo>))

    const store = useUserStore()

    expect(store.isLoggedIn).toBe(true)
    expect(store.isAdmin).toBe(true)
  })

  it('正常：fetchProfile 更新用户信息', async () => {
    const profile: UserInfo = {
      userId: 2,
      username: 'doctor1',
      realName: '张医生',
      phone: '138****5678',
      role: 'DOCTOR',
    }
    vi.mocked(get).mockResolvedValue(profile)

    const store = useUserStore()
    store.setLogin(loginData)

    const result = await store.fetchProfile()

    expect(get).toHaveBeenCalledWith('/auth/me')
    expect(result).toEqual(profile)
    expect(store.userInfo?.phone).toBe('138****5678')
    expect(JSON.parse(localStorage.getItem('cbm_user')!).phone).toBe('138****5678')
  })

  it('正常：logout 清除状态', () => {
    const store = useUserStore()
    store.setLogin(loginData)

    store.logout()

    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('cbm_token')).toBeNull()
    expect(localStorage.getItem('cbm_user')).toBeNull()
  })

  it('边界：未登录时角色判断为 false', () => {
    const store = useUserStore()

    expect(store.isLoggedIn).toBe(false)
    expect(store.isDoctor).toBe(false)
    expect(store.isInspector).toBe(false)
    expect(store.isPharmacist).toBe(false)
  })

  it('边界：各角色 computed 正确', () => {
    const store = useUserStore()

    store.setLogin({ ...loginData, role: 'INSPECTOR' })
    expect(store.isInspector).toBe(true)
    expect(store.isDoctor).toBe(false)

    store.setLogin({ ...loginData, role: 'PHARMACIST' })
    expect(store.isPharmacist).toBe(true)

    store.setLogin({ ...loginData, role: 'ADMIN' })
    expect(store.isAdmin).toBe(true)
  })
})
