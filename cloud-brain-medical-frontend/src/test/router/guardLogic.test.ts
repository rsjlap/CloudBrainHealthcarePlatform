import { describe, expect, it } from 'vitest'
import type { UserRole } from '@/types/api'

function staffHome(role: string | undefined) {
  if (role === 'ADMIN') return { name: 'admin-dashboard' }
  if (role === 'DOCTOR') return { name: 'doctor' }
  if (role === 'INSPECTOR') return { name: 'doctor-inspector' }
  if (role === 'PHARMACIST') return { name: 'doctor-pharmacist' }
  return { name: 'home' }
}

function resolveRouteGuard(input: {
  isLoggedIn: boolean
  role?: UserRole
  path: string
  requiresAuth?: boolean
  guest?: boolean
  roles?: string[]
}) {
  const { isLoggedIn, role, path, requiresAuth, guest, roles } = input

  if (!isLoggedIn) {
    if (requiresAuth) {
      return { name: 'login', query: { redirect: path } }
    }
    return true
  }

  if (role === 'PATIENT') {
    return { name: 'login', query: { reason: 'patient-disabled' }, logout: true }
  }

  if (guest) {
    return staffHome(role)
  }

  if (role === 'ADMIN' && !path.startsWith('/admin')) {
    return { name: 'admin-dashboard' }
  }

  if (roles && role && !roles.includes(role)) {
    return staffHome(role)
  }

  return true
}

describe('router guard logic', () => {
  it('正常：未登录访问受保护页跳转登录', () => {
    expect(resolveRouteGuard({
      isLoggedIn: false,
      path: '/doctor',
      requiresAuth: true,
    })).toEqual({ name: 'login', query: { redirect: '/doctor' } })
  })

  it('正常：未登录可访问 guest 页', () => {
    expect(resolveRouteGuard({
      isLoggedIn: false,
      path: '/login',
      guest: true,
    })).toBe(true)
  })

  it('正常：管理员登录后访问 guest 页跳转后台', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'ADMIN',
      path: '/login',
      guest: true,
    })).toEqual({ name: 'admin-dashboard' })
  })

  it('正常：医生登录后访问 guest 页跳转工作台', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'DOCTOR',
      path: '/login',
      guest: true,
    })).toEqual({ name: 'doctor' })
  })

  it('正常：管理员访问非 admin 路由被重定向', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'ADMIN',
      path: '/profile',
      requiresAuth: true,
    })).toEqual({ name: 'admin-dashboard' })
  })

  it('异常：患者角色登录会被强制登出', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'PATIENT',
      path: '/profile',
      requiresAuth: true,
    })).toEqual({ name: 'login', query: { reason: 'patient-disabled' }, logout: true })
  })

  it('异常：医生无权访问管理员路由', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'DOCTOR',
      path: '/admin/dashboard',
      requiresAuth: true,
      roles: ['ADMIN'],
    })).toEqual({ name: 'doctor' })
  })

  it('异常：检查医师无权访问药师路由', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'INSPECTOR',
      path: '/doctor/pharmacist',
      requiresAuth: true,
      roles: ['PHARMACIST'],
    })).toEqual({ name: 'doctor-inspector' })
  })

  it('边界：药师可访问自己的路由', () => {
    expect(resolveRouteGuard({
      isLoggedIn: true,
      role: 'PHARMACIST',
      path: '/doctor/pharmacist',
      requiresAuth: true,
      roles: ['PHARMACIST'],
    })).toBe(true)
  })
})
