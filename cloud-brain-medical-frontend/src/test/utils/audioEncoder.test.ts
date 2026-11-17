import { describe, expect, it, vi } from 'vitest'
import { convertRecordingToWav16k, formatRecordingDuration } from '@/utils/audioEncoder'

describe('audioEncoder', () => {
  it('正常：格式化录音时长', () => {
    expect(formatRecordingDuration(0)).toBe('00:00')
    expect(formatRecordingDuration(65)).toBe('01:05')
    expect(formatRecordingDuration(125)).toBe('02:05')
  })

  it('边界：不足两位补零', () => {
    expect(formatRecordingDuration(9)).toBe('00:09')
    expect(formatRecordingDuration(599)).toBe('09:59')
  })

  it('正常：转换录音为 16k WAV', async () => {
    const pcm = new Float32Array(1600).fill(0.2)
    const mockRendered = {
      numberOfChannels: 1,
      length: pcm.length,
      getChannelData: () => pcm,
    }
    const mockDecoded = {
      numberOfChannels: 1,
      duration: 0.1,
      length: pcm.length,
      getChannelData: () => pcm,
    }

    vi.stubGlobal('AudioContext', vi.fn().mockImplementation(() => ({
      decodeAudioData: vi.fn().mockResolvedValue(mockDecoded),
      close: vi.fn().mockResolvedValue(undefined),
    })))
    vi.stubGlobal('OfflineAudioContext', vi.fn().mockImplementation(() => ({
      createBufferSource: () => ({
        buffer: null as unknown,
        connect: vi.fn(),
        start: vi.fn(),
      }),
      destination: {},
      startRendering: vi.fn().mockResolvedValue(mockRendered),
    })))

    const blob = await convertRecordingToWav16k(new Blob(['fake'], { type: 'audio/webm' }))

    expect(blob.type).toBe('audio/wav')
    expect(blob.size).toBeGreaterThan(44)
  })

  it('正常：双声道混音后转换', async () => {
    const left = new Float32Array(800).fill(0.5)
    const right = new Float32Array(800).fill(-0.5)
    const mockRendered = {
      numberOfChannels: 2,
      length: 800,
      getChannelData: (ch: number) => (ch === 0 ? left : right),
    }
    const mockDecoded = {
      numberOfChannels: 2,
      duration: 0.05,
      length: 800,
      getChannelData: (ch: number) => (ch === 0 ? left : right),
    }

    vi.stubGlobal('AudioContext', vi.fn().mockImplementation(() => ({
      decodeAudioData: vi.fn().mockResolvedValue(mockDecoded),
      close: vi.fn().mockResolvedValue(undefined),
    })))
    vi.stubGlobal('OfflineAudioContext', vi.fn().mockImplementation(() => ({
      createBufferSource: () => ({
        buffer: null as unknown,
        connect: vi.fn(),
        start: vi.fn(),
      }),
      destination: {},
      startRendering: vi.fn().mockResolvedValue(mockRendered),
    })))

    const blob = await convertRecordingToWav16k(new Blob(['fake'], { type: 'audio/webm' }))

    expect(blob.size).toBeGreaterThan(44)
  })
})
