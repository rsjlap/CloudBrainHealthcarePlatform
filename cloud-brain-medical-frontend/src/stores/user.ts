import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse, UserInfo } from '@/types/api'
import { get } from '@/utils/request'

const TOKEN_KEY = 'cbm_token'
const USER_KEY = 'cbm_user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)!) : null,
  )

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isDoctor = computed(() => userInfo.value?.role === 'DOCTOR')
  const isInspector = computed(() => userInfo.value?.role === 'INSPECTOR')
  const isPharmacist = computed(() => userInfo.value?.role === 'PHARMACIST')

  function setLogin(data: LoginResponse) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      phone: '',
      role: data.role,
    }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
  }

  async function fetchProfile() {
    const profile = await get<UserInfo>('/auth/me')
    userInfo.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, isLoggedIn, isAdmin, isDoctor, isInspector, isPharmacist, setLogin, fetchProfile, logout }
})
