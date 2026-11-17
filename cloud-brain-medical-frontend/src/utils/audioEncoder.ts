const TARGET_SAMPLE_RATE = 16000

function encodeWav(pcm: Int16Array): Blob {
  const buffer = new ArrayBuffer(44 + pcm.length * 2)
  const view = new DataView(buffer)

  const writeString = (offset: number, text: string) => {
    for (let i = 0; i < text.length; i += 1) {
      view.setUint8(offset + i, text.charCodeAt(i))
    }
  }

  writeString(0, 'RIFF')
  view.setUint32(4, 36 + pcm.length * 2, true)
  writeString(8, 'WAVE')
  writeString(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, TARGET_SAMPLE_RATE, true)
  view.setUint32(28, TARGET_SAMPLE_RATE * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(36, 'data')
  view.setUint32(40, pcm.length * 2, true)

  let offset = 44
  for (let i = 0; i < pcm.length; i += 1) {
    view.setInt16(offset, pcm[i]!, true)
    offset += 2
  }

  return new Blob([buffer], { type: 'audio/wav' })
}

function mixToMono(buffer: AudioBuffer): Float32Array {
  if (buffer.numberOfChannels === 1) {
    return buffer.getChannelData(0)
  }
  const length = buffer.length
  const mixed = new Float32Array(length)
  for (let ch = 0; ch < buffer.numberOfChannels; ch += 1) {
    const channel = buffer.getChannelData(ch)
    for (let i = 0; i < length; i += 1) {
      mixed[i]! += channel[i]! / buffer.numberOfChannels
    }
  }
  return mixed
}

function floatToInt16(samples: Float32Array): Int16Array {
  const pcm = new Int16Array(samples.length)
  for (let i = 0; i < samples.length; i += 1) {
    const sample = Math.max(-1, Math.min(1, samples[i]!))
    pcm[i] = sample < 0 ? sample * 0x8000 : sample * 0x7fff
  }
  return pcm
}

export async function convertRecordingToWav16k(blob: Blob): Promise<Blob> {
  const arrayBuffer = await blob.arrayBuffer()
  const audioContext = new AudioContext()
  try {
    const decoded = await audioContext.decodeAudioData(arrayBuffer.slice(0))
    const offline = new OfflineAudioContext(1, Math.ceil(decoded.duration * TARGET_SAMPLE_RATE), TARGET_SAMPLE_RATE)
    const source = offline.createBufferSource()
    source.buffer = decoded
    source.connect(offline.destination)
    source.start(0)
    const rendered = await offline.startRendering()
    const pcm = floatToInt16(mixToMono(rendered))
    return encodeWav(pcm)
  } finally {
    await audioContext.close()
  }
}

export function formatRecordingDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
