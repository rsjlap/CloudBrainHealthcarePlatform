import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

type InterceptorHandler = (value: Record<string, unknown>) => unknown

function createMockAxiosInstance() {
  const requestHandlers: InterceptorHandler[] = []
  const responseSuccessHandlers: InterceptorHandler[] = []
  const responseErrorHandlers: InterceptorHandler[] = []

  const instance = {
    interceptors: {
      request: {
        use: (handler: InterceptorHandler) => {
          requestHandlers.push(handler)
        },
      },
      response: {
        use: (onSuccess: InterceptorHandler, onError: InterceptorHandler) => {
          responseSuccessHandlers.push(onSuccess)
          responseErrorHandlers.push(onError)
        },
      },
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    requestHandlers,
    responseSuccessHandlers,
    responseErrorHandlers,
  }

  return instance
}

const mockInstance = createMockAxiosInstance()

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockInstance),
  },
}))

describe('request', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.mocked(ElMessage.error).mockClear()
    mockInstance.requestHandlers.length = 0
    mockInstance.responseSuccessHandlers.length = 0
    mockInstance.responseErrorHandlers.length = 0
    vi.resetModules()
    await import('@/utils/request')
  })

  it('正常：响应 code=200 时返回 data', () => {
    const handler = mockInstance.responseSuccessHandlers[0]!
    const result = handler({
      data: { code: 200, data: { id: 1 }, message: 'ok' },
      config: {},
    })

    expect(result).toEqual({ id: 1 })
  })

  it('异常：业务错误弹出提示并 reject', async () => {
    const handler = mockInstance.responseSuccessHandlers[0]!

    await expect(handler({
      data: { code: 500, message: '服务器错误' },
      config: {},
    }) as Promise<unknown>).rejects.toThrow('服务器错误')
    expect(ElMessage.error).toHaveBeenCalledWith('服务器错误')
  })

  it('异常：401 清除登录态', async () => {
    localStorage.setItem('cbm_token', 'old')
    localStorage.setItem('cbm_user', '{}')
    Object.defineProperty(window, 'location', {
      value: { pathname: '/doctor', href: '' },
      writable: true,
    })

    const handler = mockInstance.responseSuccessHandlers[0]!

    await expect(handler({
      data: { code: 401, message: '未登录' },
      config: {},
    }) as Promise<unknown>).rejects.toThrow()
    expect(localStorage.getItem('cbm_token')).toBeNull()
    expect(localStorage.getItem('cbm_user')).toBeNull()
  })

  it('边界：skipErrorToast 时不弹错误', async () => {
    const handler = mockInstance.responseSuccessHandlers[0]!

    await expect(handler({
      data: { code: 400, message: '参数错误' },
      config: { skipErrorToast: true },
    }) as Promise<unknown>).rejects.toThrow('参数错误')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('正常：请求拦截器附加 Authorization', () => {
    localStorage.setItem('cbm_token', 'abc123')
    const handler = mockInstance.requestHandlers[0]!

    const config = handler({ headers: {} }) as { headers: Record<string, string> }

    expect(config.headers.Authorization).toBe('Bearer abc123')
  })

  it('异常：网络错误提示', async () => {
    const handler = mockInstance.responseErrorHandlers[0]!

    await expect(handler({ message: 'Network Error', config: {} })).rejects.toEqual({
      message: 'Network Error',
      config: {},
    })
    expect(ElMessage.error).toHaveBeenCalledWith('Network Error')
  })

  it('异常：网络错误且 skipErrorToast 时不弹窗', async () => {
    const handler = mockInstance.responseErrorHandlers[0]!

    await expect(handler({ message: 'Network Error', config: { skipErrorToast: true } })).rejects.toEqual({
      message: 'Network Error',
      config: { skipErrorToast: true },
    })
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('正常：封装 get/post/put/del 方法', async () => {
    const { get, post, put, del } = await import('@/utils/request')
    mockInstance.get.mockResolvedValue({ id: 1 })
    mockInstance.post.mockResolvedValue({ ok: true })
    mockInstance.put.mockResolvedValue({ ok: true })
    mockInstance.delete.mockResolvedValue({ ok: true })

    await expect(get('/a')).resolves.toEqual({ id: 1 })
    await expect(post('/b', { x: 1 })).resolves.toEqual({ ok: true })
    await expect(put('/c', { y: 2 })).resolves.toEqual({ ok: true })
    await expect(del('/d')).resolves.toEqual({ ok: true })
  })
})
