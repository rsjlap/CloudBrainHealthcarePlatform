import { describe, expect, it } from 'vitest'
import {
  formatKgText,
  hasValidKgReference,
  isKgPlaceholder,
  parseKgReference,
  splitKgAnswerLines,
} from '@/utils/kgReference'

describe('kgReference', () => {
  it('正常：格式化转义字符', () => {
    expect(formatKgText('第一行\\n第二行\\t缩进')).toBe('第一行\n第二行\t缩进')
    expect(formatKgText('  内容  ')).toBe('内容')
  })

  it('边界：占位符判定', () => {
    expect(isKgPlaceholder('')).toBe(true)
    expect(isKgPlaceholder('success')).toBe(true)
    expect(isKgPlaceholder('OK')).toBe(true)
    expect(isKgPlaceholder('failed')).toBe(true)
    expect(isKgPlaceholder('有效图谱内容')).toBe(false)
  })

  it('正常：解析纯文本回复', () => {
    const parsed = parseKgReference('建议完善血常规检查')

    expect(parsed.answer).toBe('建议完善血常规检查')
    expect(hasValidKgReference('建议完善血常规检查')).toBe(true)
  })

  it('正常：解析 JSON 对象', () => {
    const raw = JSON.stringify({
      answer: '推荐检查项目',
      disease: '高血压',
      question: '下一步检查？',
    })
    const parsed = parseKgReference(raw)

    expect(parsed.answer).toBe('推荐检查项目')
    expect(parsed.meta).toContain('高血压')
    expect(parsed.question).toBe('下一步检查？')
  })

  it('正常：解析嵌套 data 包装', () => {
    const raw = JSON.stringify({
      data: { answer: '图谱结论', disease: '糖尿病' },
    })

    expect(parseKgReference(raw).answer).toBe('图谱结论')
  })

  it('正常：解析数组结构', () => {
    const raw = JSON.stringify(['项目A', '项目B'])

    expect(parseKgReference(raw).answer).toBe('项目A\n项目B')
  })

  it('异常：损坏 JSON 尝试字段提取', () => {
    const raw = '{"answer":"部分提取成功","disease":"感冒'
    const parsed = parseKgReference(raw)

    expect(parsed.answer).toBe('部分提取成功')
  })

  it('正常：解析含检查/药品列表的 JSON', () => {
    const raw = JSON.stringify({
      exams: [{ examType: '血常规', bodyPart: '手臂' }],
      drugs: [{ drugName: '阿莫西林' }],
      forbidden: ['青霉素'],
    })
    const parsed = parseKgReference(raw)

    expect(parsed.answer).toContain('血常规')
    expect(parsed.answer).toContain('阿莫西林')
    expect(parsed.answer).toContain('青霉素')
  })

  it('正常：解析 message 兜底字段', () => {
    const raw = JSON.stringify({ message: '图谱服务返回说明' })

    expect(parseKgReference(raw).answer).toBe('图谱服务返回说明')
  })

  it('正常：splitKgAnswerLines 按编号与分号拆分', () => {
    expect(splitKgAnswerLines('1. 第一项\n2. 第二项')).toEqual(['1. 第一项', '2. 第二项'])
    const longText = `${'建议内容'.repeat(20)}；第二段；第三段`
    expect(splitKgAnswerLines(longText).length).toBeGreaterThan(1)
  })

  it('边界：空值与占位符返回空答案', () => {
    expect(parseKgReference(null).answer).toBe('')
    expect(parseKgReference('success').answer).toBe('')
    expect(hasValidKgReference('success')).toBe(false)
  })

  it('正常：splitKgAnswerLines 拆分条目', () => {
    expect(splitKgAnswerLines('· 条目一\n· 条目二')).toEqual(['条目一', '条目二'])
    expect(splitKgAnswerLines('1. 第一项\n2. 第二项')).toEqual(['1. 第一项', '2. 第二项'])
    expect(splitKgAnswerLines('')).toEqual([])
  })

  it('边界：长句按分号拆分', () => {
    const text = `${'长内容'.repeat(30)}；第二段；第三段`

    expect(splitKgAnswerLines(text).length).toBeGreaterThan(1)
  })
})
