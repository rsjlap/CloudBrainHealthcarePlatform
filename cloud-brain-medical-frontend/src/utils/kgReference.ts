export interface ParsedKgReference {
  answer: string
  question?: string
  meta?: string
}

const WRAPPER_KEYS = ['data', 'result', 'payload'] as const

const TEXT_CONTENT_KEYS = [
  'answer',
  'raw',
  'result',
  'content',
  'text',
  'response',
  'reply',
] as const

const FALLBACK_TEXT_KEYS = ['message', 'msg'] as const

const QUESTION_KEYS = ['question', 'query', 'prompt'] as const

const FIELD_LABELS: Record<string, string> = {
  disease: '相关疾病',
  raw: '图谱回复',
  answer: '图谱回复',
  result: '图谱回复',
  content: '图谱回复',
  exams: '检查项目',
  drugs: '相关药品',
  forbidden: '用药禁忌',
}

export function formatKgText(text: string): string {
  return text
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t')
    .replace(/\\"/g, '"')
    .replace(/\r\n/g, '\n')
    .trim()
}

export function isKgPlaceholder(text?: string | null): boolean {
  if (!text?.trim()) return true
  const normalized = text.trim().toLowerCase()
  return normalized === 'success'
    || normalized === 'ok'
    || normalized === 'true'
    || normalized === 'failed'
    || normalized === 'fail'
    || normalized === 'error'
}

export function hasValidKgReference(raw?: string | null): boolean {
  const parsed = parseKgReference(raw)
  return !!parsed.answer && !isKgPlaceholder(parsed.answer)
}

export function parseKgReference(raw?: string | null): ParsedKgReference {
  if (!raw?.trim()) {
    return { answer: '' }
  }

  const trimmed = raw.trim()
  const parsed = tryParseJson(trimmed)
  if (parsed !== null) {
    return formatKgValue(parsed)
  }

  const extracted = extractFromBrokenJson(trimmed)
  if (extracted) {
    return extracted
  }

  return { answer: isKgPlaceholder(trimmed) ? '' : formatKgText(trimmed) }
}

function tryParseJson(text: string): unknown | null {
  if (!text.startsWith('{') && !text.startsWith('[')) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function extractFromBrokenJson(text: string): ParsedKgReference | null {
  if (!text.startsWith('{')) {
    return null
  }

  for (const key of TEXT_CONTENT_KEYS) {
    const match = text.match(new RegExp(`"${key}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`, 's'))
    if (match?.[1]) {
      const answer = formatKgText(match[1])
      if (answer) {
        const diseaseMatch = text.match(/"disease"\s*:\s*"((?:\\.|[^"\\])*)"/)
        return {
          answer,
          meta: diseaseMatch?.[1] ? `相关疾病：${formatKgText(diseaseMatch[1])}` : undefined,
        }
      }
    }
  }
  return null
}

function formatKgValue(value: unknown): ParsedKgReference {
  if (value === null || value === undefined) {
    return { answer: '' }
  }

  if (typeof value === 'string') {
    const nested = tryParseJson(value.trim())
    if (nested !== null) {
      return formatKgValue(nested)
    }
    const answer = formatKgText(value)
    return { answer: isKgPlaceholder(answer) ? '' : answer }
  }

  if (Array.isArray(value)) {
    const lines = value.flatMap((item) => formatArrayItem(item))
    return { answer: lines.join('\n') }
  }

  if (typeof value === 'object') {
    return formatKgRecord(value as Record<string, unknown>)
  }

  return { answer: String(value) }
}

function formatKgRecord(record: Record<string, unknown>): ParsedKgReference {
  const question = pickTextField(record, QUESTION_KEYS)
  const metaParts: string[] = []

  if (typeof record.disease === 'string' && record.disease.trim()) {
    metaParts.push(`相关疾病：${formatKgText(record.disease)}`)
  }

  for (const key of WRAPPER_KEYS) {
    const wrapped = record[key]
    if (wrapped && typeof wrapped === 'object') {
      const nested = formatKgValue(wrapped)
      if (nested.answer && !isKgPlaceholder(nested.answer)) {
        return {
          answer: nested.answer,
          question: question || nested.question || undefined,
          meta: metaParts.join('\n') || nested.meta || undefined,
        }
      }
    }
  }

  for (const key of TEXT_CONTENT_KEYS) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) {
      const nested = tryParseJson(value.trim())
      const answer = nested !== null ? formatKgValue(nested).answer : formatKgText(value)
      if (answer && !isKgPlaceholder(answer)) {
        return {
          answer,
          question: question || undefined,
          meta: metaParts.join('\n') || undefined,
        }
      }
    }
  }

  for (const key of FALLBACK_TEXT_KEYS) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) {
      const answer = formatKgText(value)
      if (answer && !isKgPlaceholder(answer)) {
        return {
          answer,
          question: question || undefined,
          meta: metaParts.join('\n') || undefined,
        }
      }
    }
  }

  const listSections: string[] = []
  appendListSection(listSections, '检查项目', record.exams)
  appendListSection(listSections, '相关药品', record.drugs)
  appendListSection(listSections, '用药禁忌', record.forbidden)

  if (listSections.length) {
    return {
      answer: listSections.join('\n'),
      question: question || undefined,
      meta: metaParts.join('\n') || undefined,
    }
  }

  const lines: string[] = []
  for (const [key, value] of Object.entries(record)) {
    if (QUESTION_KEYS.includes(key as (typeof QUESTION_KEYS)[number])) continue
    if (value === null || value === undefined || value === '') continue
    if (Array.isArray(value) && value.length === 0) continue
    if (key === 'disease') continue
    const line = formatFieldLine(key, value)
    if (line) lines.push(line)
  }

  return {
    answer: lines.join('\n') || '',
    question: question || undefined,
    meta: metaParts.join('\n') || undefined,
  }
}

function appendListSection(sections: string[], label: string, value: unknown) {
  if (!Array.isArray(value) || value.length === 0) return
  const items = value.flatMap((item) => formatArrayItem(item))
  if (items.length) {
    sections.push(`${label}：`)
    sections.push(...items.map((item) => `· ${item}`))
  }
}

function formatFieldLine(key: string, value: unknown): string {
  const label = FIELD_LABELS[key] || key
  if (typeof value === 'string') {
    return `${label}：${formatKgText(value)}`
  }
  if (Array.isArray(value)) {
    const items = value.flatMap((item) => formatArrayItem(item))
    return items.length ? `${label}：\n${items.map((item) => `· ${item}`).join('\n')}` : ''
  }
  if (typeof value === 'object' && value !== null) {
    const inner = formatKgValue(value).answer
    return inner ? `${label}：${inner}` : ''
  }
  return `${label}：${String(value)}`
}

function formatArrayItem(item: unknown): string[] {
  if (typeof item === 'string') {
    const text = formatKgText(item)
    return text ? [text] : []
  }
  if (item && typeof item === 'object') {
    const obj = item as Record<string, unknown>
    const name = [obj.name, obj.itemName, obj.drugName, obj.examType, obj.title]
      .find((v) => typeof v === 'string' && v.trim()) as string | undefined
    if (name) {
      const extra = [obj.bodyPart, obj.category, obj.description]
        .filter((v) => typeof v === 'string' && v.trim())
        .join(' · ')
      return extra ? [`${name}（${extra}）`] : [name]
    }
    const inner = formatKgValue(item).answer
    return inner ? [inner] : []
  }
  return [String(item)]
}

function pickTextField(record: Record<string, unknown>, keys: readonly string[]): string {
  for (const key of keys) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) {
      return formatKgText(value)
    }
  }
  return ''
}

/** 将长段落拆成易读条目 */
export function splitKgAnswerLines(text: string): string[] {
  const normalized = formatKgText(text)
  if (!normalized) return []

  const bulletLines = normalized
    .split('\n')
    .map((line) => line.replace(/^[·•-]\s*/, '').trim())
    .filter(Boolean)
  if (bulletLines.length > 1) return bulletLines

  const numberedParts = normalized
    .split(/(?=\d+[.、．]\s)/)
    .map((part) => part.replace(/^\d+[.、．]\s*/, '').trim())
    .filter(Boolean)
  if (numberedParts.length > 1) return numberedParts

  if (normalized.includes('\n')) {
    const lines = normalized.split('\n').map((line) => line.trim()).filter(Boolean)
    if (lines.length > 1) return lines
  }

  if (normalized.length > 80 && normalized.includes('；')) {
    return normalized.split('；').map((part) => part.trim()).filter(Boolean)
  }

  return [normalized]
}
