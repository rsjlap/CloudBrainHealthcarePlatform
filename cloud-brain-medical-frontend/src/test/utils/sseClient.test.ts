import { describe, expect, it, vi } from 'vitest'
import { postSse } from '@/utils/sseClient'

function buildSseResponse(chunks: string[]) {
  let index = 0
  const encoder = new TextEncoder()
  return {
    ok: true,
    body: {
      getReader: () => ({
        read: vi.fn(async () => {
          if (index >= chunks.length) {
            return { done: true, value: undefined }
          }
          const value = encoder.encode(chunks[index])
          index += 1
          return { done: false, value }
        }),
      }),
    },
  }
}

describe('sseClient', () => {
  it('正常：解析 delta / status / complete 事件', async () => {
    const onDelta = vi.fn()
    const onStatus = vi.fn()
    const onComplete = vi.fn()
    const onError = vi.fn()

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      buildSseResponse([
        'event: status\ndata: {"message":"生成中"}\n\n',
        'event: delta\ndata: {"text":"头"}\n\n',
        'event: complete\ndata: {"chiefComplaint":"头痛"}\n\n',
      ]),
    ))

    await postSse('/ai/medical-record/stream', { text: 'test' }, {
      onDelta,
      onStatus,
      onComplete,
      onError,
    })

    expect(onStatus).toHaveBeenCalledWith('生成中')
    expect(onDelta).toHaveBeenCalledWith('头')
    expect(onComplete).toHaveBeenCalledWith({ chiefComplaint: '头痛' })
    expect(onError).not.toHaveBeenCalled()
  })

  it('异常：HTTP 错误触发 onError 并抛出异常', async () => {
    const onError = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: '服务不可用' }),
    }))

    await expect(postSse('/ai/test', {}, { onError })).rejects.toThrow('服务不可用')
    expect(onError).toHaveBeenCalledWith('服务不可用')
  })

  it('异常：响应体不可读', async () => {
    const onError = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, body: null }))

    await expect(postSse('/ai/test', {}, { onError })).rejects.toThrow('浏览器不支持流式响应')
    expect(onError).toHaveBeenCalledWith('浏览器不支持流式响应')
  })

  it('异常：SSE error 事件', async () => {
    const onError = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      buildSseResponse(['event: error\ndata: {"message":"AI失败"}\n\n']),
    ))

    await postSse('/ai/test', {}, { onError })

    expect(onError).toHaveBeenCalledWith('AI失败')
  })

  it('边界：complete 后忽略后续 error', async () => {
    const onComplete = vi.fn()
    const onError = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      buildSseResponse([
        'event: complete\ndata: {"ok":true}\n\n',
        'event: error\ndata: {"message":"late error"}\n\n',
      ]),
    ))

    await postSse('/ai/test', {}, { onComplete, onError })

    expect(onComplete).toHaveBeenCalled()
    expect(onError).not.toHaveBeenCalled()
  })

  it('边界：delta 事件使用纯文本', async () => {
    const onDelta = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      buildSseResponse(['event: delta\ndata: 纯文本增量\n\n']),
    ))

    await postSse('/ai/test', {}, { onDelta })

    expect(onDelta).toHaveBeenCalledWith('纯文本增量')
  })

  it('边界：携带 Authorization 请求头', async () => {
    localStorage.setItem('cbm_token', 'test-token')
    const fetchMock = vi.fn().mockResolvedValue(
      buildSseResponse(['event: complete\ndata: {}\n\n']),
    )
    vi.stubGlobal('fetch', fetchMock)

    await postSse('/ai/test', { a: 1 }, {})

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/ai/test'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    )
  })
})
