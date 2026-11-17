import { beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  vi.restoreAllMocks()
})
