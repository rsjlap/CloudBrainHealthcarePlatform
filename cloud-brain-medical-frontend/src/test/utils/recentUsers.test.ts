import { describe, expect, it } from 'vitest'
import {
  getLastRecentUsername,
  loadRecentUsers,
  removeRecentUser,
  roleLabels,
  saveRecentUser,
} from '@/utils/recentUsers'

describe('recentUsers', () => {
  it('正常：保存并读取最近登录用户', () => {
    saveRecentUser({ username: 'doctor1', realName: '张医生', role: 'DOCTOR' })

    const list = loadRecentUsers()

    expect(list).toHaveLength(1)
    expect(list[0]?.username).toBe('doctor1')
    expect(list[0]?.realName).toBe('张医生')
    expect(getLastRecentUsername()).toBe('doctor1')
  })

  it('正常：同一用户再次登录排到最前', () => {
    saveRecentUser({ username: 'a', role: 'ADMIN' })
    saveRecentUser({ username: 'b', role: 'DOCTOR' })
    saveRecentUser({ username: 'a', role: 'ADMIN' })

    const list = loadRecentUsers()

    expect(list[0]?.username).toBe('a')
    expect(list[1]?.username).toBe('b')
  })

  it('边界：最多保留 8 个用户', () => {
    for (let i = 0; i < 10; i += 1) {
      saveRecentUser({ username: `user${i}`, role: 'DOCTOR' })
    }

    expect(loadRecentUsers()).toHaveLength(8)
    expect(loadRecentUsers()[0]?.username).toBe('user9')
  })

  it('正常：删除指定用户', () => {
    saveRecentUser({ username: 'doctor1', role: 'DOCTOR' })
    saveRecentUser({ username: 'admin', role: 'ADMIN' })

    removeRecentUser('doctor1')

    expect(loadRecentUsers()).toHaveLength(1)
    expect(loadRecentUsers()[0]?.username).toBe('admin')
  })

  it('边界：无记录时返回空', () => {
    expect(loadRecentUsers()).toEqual([])
    expect(getLastRecentUsername()).toBe('')
  })

  it('异常：localStorage 数据损坏时返回空数组', () => {
    localStorage.setItem('cbm_recent_users', '{invalid')

    expect(loadRecentUsers()).toEqual([])
  })

  it('异常：localStorage 不是数组时返回空数组', () => {
    localStorage.setItem('cbm_recent_users', JSON.stringify({ foo: 1 }))

    expect(loadRecentUsers()).toEqual([])
  })

  it('边界：角色标签映射完整', () => {
    expect(roleLabels.ADMIN).toBe('管理员')
    expect(roleLabels.DOCTOR).toBe('医生')
    expect(roleLabels.PATIENT).toBe('患者')
    expect(roleLabels.PHARMACIST).toBe('药师')
    expect(roleLabels.INSPECTOR).toBe('检查医师')
  })
})
