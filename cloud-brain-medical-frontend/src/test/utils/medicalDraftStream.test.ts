import { describe, expect, it } from 'vitest'
import {
  MEDICAL_STREAM_STEPS,
  draftToDisplayFields,
  extractPartialDraftFields,
  normalizeMedicalDraft,
  resolveStreamStep,
} from '@/utils/medicalDraftStream'

describe('medicalDraftStream', () => {
  it('正常：从完整 draft 生成展示字段', () => {
    const fields = draftToDisplayFields({
      chiefComplaint: '头痛3天',
      diagnosis: '紧张性头痛',
      temperature: '36.5',
      pulse: '78',
    })

    expect(fields.some((f) => f.key === 'chiefComplaint' && f.value === '头痛3天')).toBe(true)
    expect(fields.some((f) => f.key === 'vitals' && f.value.includes('体温 36.5℃'))).toBe(true)
  })

  it('边界：空 draft 返回空列表', () => {
    expect(draftToDisplayFields(null)).toEqual([])
    expect(draftToDisplayFields(undefined)).toEqual([])
    expect(draftToDisplayFields({})).toEqual([])
  })

  it('正常：从不完整 JSON 流中提取字段', () => {
    const raw = '{"chiefComplaint":"咳嗽","diagnosis":"上呼吸道感染","temperature":"37.2"}'
    const fields = extractPartialDraftFields(raw)

    expect(fields.find((f) => f.key === 'chiefComplaint')?.value).toBe('咳嗽')
    expect(fields.find((f) => f.key === 'vitals')?.value).toContain('体温 37.2℃')
  })

  it('边界：无匹配字段时返回空', () => {
    expect(extractPartialDraftFields('{"unknown":"x"}')).toEqual([])
    expect(extractPartialDraftFields('')).toEqual([])
  })

  it('正常：normalizeMedicalDraft 解析 JSON 字符串', () => {
    const draft = normalizeMedicalDraft('{"chiefComplaint":"腹痛"}')

    expect(draft?.chiefComplaint).toBe('腹痛')
  })

  it('正常：normalizeMedicalDraft 接受对象', () => {
    const draft = normalizeMedicalDraft({ diagnosis: '胃炎' })

    expect(draft?.diagnosis).toBe('胃炎')
  })

  it('异常：非法 JSON 字符串返回 null', () => {
    expect(normalizeMedicalDraft('{bad json')).toBeNull()
    expect(normalizeMedicalDraft(null)).toBeNull()
    expect(normalizeMedicalDraft(123)).toBeNull()
  })

  it('正常：resolveStreamStep 识别各阶段', () => {
    expect(resolveStreamStep(undefined)).toBe(0)
    expect(resolveStreamStep('整理口述内容')).toBe(0)
    expect(resolveStreamStep('AI 生成草稿中')).toBe(1)
    expect(resolveStreamStep('融合知识图谱')).toBe(2)
    expect(resolveStreamStep('生成完成，请预览')).toBe(3)
  })

  it('边界：流式步骤常量完整', () => {
    expect(MEDICAL_STREAM_STEPS).toHaveLength(4)
    expect(MEDICAL_STREAM_STEPS[3]).toBe('生成完成')
  })
})
