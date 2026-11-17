import type { AiMedicalDraft } from '@/types/api'

export interface MedicalDraftField {
  key: keyof AiMedicalDraft | 'vitals'
  label: string
  value: string
}

const STRING_FIELDS: { key: keyof AiMedicalDraft; label: string }[] = [
  { key: 'chiefComplaint', label: '主诉' },
  { key: 'presentIllness', label: '现病史' },
  { key: 'pastHistory', label: '既往史' },
  { key: 'allergyHistory', label: '过敏史' },
  { key: 'personalHistory', label: '个人史' },
  { key: 'maritalHistory', label: '婚育史' },
  { key: 'familyHistory', label: '家族史' },
  { key: 'physicalExam', label: '体格检查' },
  { key: 'auxiliaryExam', label: '辅助检查' },
  { key: 'diagnosis', label: '初步诊断' },
  { key: 'treatmentPlan', label: '处理意见' },
]

function decodeJsonString(value: string): string {
  return value
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t')
    .replace(/\\"/g, '"')
    .replace(/\\\\/g, '\\')
    .trim()
}

function pickStringField(raw: string, key: string): string {
  const match = raw.match(new RegExp(`"${key}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`, 's'))
  return match?.[1] ? decodeJsonString(match[1]) : ''
}

export function extractPartialDraftFields(raw: string): MedicalDraftField[] {
  const fields: MedicalDraftField[] = []
  for (const item of STRING_FIELDS) {
    const value = pickStringField(raw, item.key)
    if (value) {
      fields.push({ key: item.key, label: item.label, value })
    }
  }

  const temperature = pickStringField(raw, 'temperature')
  const respiration = pickStringField(raw, 'respiration')
  const pulse = pickStringField(raw, 'pulse')
  const bloodPressure = pickStringField(raw, 'bloodPressure')
  const vitals = [
    temperature ? `体温 ${temperature}℃` : '',
    respiration ? `呼吸 ${respiration}次/分` : '',
    pulse ? `脉搏 ${pulse}次/分` : '',
    bloodPressure ? `血压 ${bloodPressure}mmHg` : '',
  ].filter(Boolean).join('；')
  if (vitals) {
    fields.splice(6, 0, { key: 'vitals', label: '生命体征', value: vitals })
  }

  return fields
}

export function draftToDisplayFields(draft: Partial<AiMedicalDraft> | null | undefined): MedicalDraftField[] {
  if (!draft) return []
  const fields: MedicalDraftField[] = []
  for (const item of STRING_FIELDS) {
    const value = draft[item.key]
    if (typeof value === 'string' && value.trim()) {
      fields.push({ key: item.key, label: item.label, value: value.trim() })
    }
  }
  const vitals = [
    draft.temperature ? `体温 ${draft.temperature}℃` : '',
    draft.respiration ? `呼吸 ${draft.respiration}次/分` : '',
    draft.pulse ? `脉搏 ${draft.pulse}次/分` : '',
    draft.bloodPressure ? `血压 ${draft.bloodPressure}mmHg` : '',
  ].filter(Boolean).join('；')
  if (vitals) {
    fields.splice(6, 0, { key: 'vitals', label: '生命体征', value: vitals })
  }
  return fields
}

export function normalizeMedicalDraft(payload: unknown): AiMedicalDraft | null {
  if (!payload) return null
  if (typeof payload === 'string') {
    try {
      return JSON.parse(payload) as AiMedicalDraft
    } catch {
      return null
    }
  }
  if (typeof payload === 'object') {
    return payload as AiMedicalDraft
  }
  return null
}

export const MEDICAL_STREAM_STEPS = [
  '整理口述内容',
  '生成病历草稿',
  '融合知识图谱',
  '生成完成',
] as const

export function resolveStreamStep(status?: string): number {
  if (!status) return 0
  if (status.includes('图谱') || status.includes('KG') || status.includes('校验')) return 2
  if (status.includes('完成') || status.includes('预览')) return 3
  if (status.includes('生成') || status.includes('草稿') || status.includes('AI')) return 1
  return 0
}
