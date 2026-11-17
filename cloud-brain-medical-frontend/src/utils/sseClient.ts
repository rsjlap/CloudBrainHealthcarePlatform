const TOKEN_KEY = 'cbm_token'

export interface PostSseHandlers {
  onDelta?: (text: string) => void
  onStatus?: (message: string) => void
  onComplete?: (data: unknown) => void
  onError?: (message: string) => void
}

function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || '/api/v1'
}

function parseSseBlock(block: string): { event: string; data: string } | null {
  const lines = block.split('\n').filter(Boolean)
  if (!lines.length) return null
  let event = 'message'
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (!dataLines.length) return null
  return { event, data: dataLines.join('\n') }
}

function dispatchSseEvent(event: string, data: string, handlers: PostSseHandlers) {
  let payload: unknown = data
  try {
    payload = JSON.parse(data)
  } catch {
    // keep raw text
  }

  if (event === 'delta') {
    const text = typeof payload === 'object' && payload !== null && 'text' in payload
      ? String((payload as { text?: string }).text ?? '')
      : data
    if (text) handlers.onDelta?.(text)
    return
  }
  if (event === 'status') {
    const message = typeof payload === 'object' && payload !== null && 'message' in payload
      ? String((payload as { message?: string }).message ?? '')
      : data
    if (message) handlers.onStatus?.(message)
    return
  }
  if (event === 'complete') {
    let draft = payload
    if (typeof payload === 'string') {
      try {
        draft = JSON.parse(payload)
      } catch {
        draft = payload
      }
    }
    handlers.onComplete?.(draft)
    return
  }
  if (event === 'error') {
    const message = typeof payload === 'object' && payload !== null && 'message' in payload
      ? String((payload as { message?: string }).message ?? '请求失败')
      : data || '请求失败'
    handlers.onError?.(message)
  }
}

export async function postSse(
  path: string,
  body: unknown,
  handlers: PostSseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  let streamCompleted = false
  const guardedHandlers: PostSseHandlers = {
    onDelta: handlers.onDelta,
    onStatus: handlers.onStatus,
    onComplete: (data) => {
      streamCompleted = true
      handlers.onComplete?.(data)
    },
    onError: (message) => {
      if (streamCompleted) return
      handlers.onError?.(message)
    },
  }

  const token = localStorage.getItem(TOKEN_KEY)
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
    signal,
  })

  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const json = await response.json() as { message?: string }
      if (json.message) message = json.message
    } catch {
      // ignore
    }
    guardedHandlers.onError?.(message)
    throw new Error(message)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    const message = '浏览器不支持流式响应'
    guardedHandlers.onError?.(message)
    throw new Error(message)
  }

  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let splitIndex = buffer.indexOf('\n\n')
      while (splitIndex >= 0) {
        const block = buffer.slice(0, splitIndex)
        buffer = buffer.slice(splitIndex + 2)
        const parsed = parseSseBlock(block)
        if (parsed) {
          dispatchSseEvent(parsed.event, parsed.data, guardedHandlers)
        }
        splitIndex = buffer.indexOf('\n\n')
      }
    }

    if (buffer.trim()) {
      const parsed = parseSseBlock(buffer.trim())
      if (parsed) {
        dispatchSseEvent(parsed.event, parsed.data, guardedHandlers)
      }
    }
  } catch (err) {
    if (streamCompleted) return
    throw err
  }
}
