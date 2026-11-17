import { onUnmounted, ref } from 'vue'
import { formatRecordingDuration } from '@/utils/audioEncoder'

export function useAudioRecorder(maxDurationSec = 180) {
  const recording = ref(false)
  const durationSec = ref(0)
  const error = ref('')

  let mediaRecorder: MediaRecorder | null = null
  let mediaStream: MediaStream | null = null
  let chunks: Blob[] = []
  let timer: number | undefined

  function clearTimer() {
    if (timer) {
      window.clearInterval(timer)
      timer = undefined
    }
  }

  function releaseStream() {
    mediaStream?.getTracks().forEach((track) => track.stop())
    mediaStream = null
  }

  async function start() {
    error.value = ''
    if (recording.value) return

    if (!navigator.mediaDevices?.getUserMedia) {
      error.value = '当前浏览器不支持录音，请使用 Chrome 或 Edge'
      throw new Error(error.value)
    }

    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    chunks = []
    durationSec.value = 0

    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : MediaRecorder.isTypeSupported('audio/webm')
        ? 'audio/webm'
        : ''

    mediaRecorder = mimeType
      ? new MediaRecorder(mediaStream, { mimeType })
      : new MediaRecorder(mediaStream)

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data)
      }
    }

    mediaRecorder.start(250)
    recording.value = true
    timer = window.setInterval(() => {
      durationSec.value += 1
      if (durationSec.value >= maxDurationSec) {
        stop().catch(() => undefined)
      }
    }, 1000)
  }

  async function stop(): Promise<Blob | null> {
    clearTimer()
    if (!mediaRecorder || !recording.value) {
      return null
    }

    recording.value = false
    const recorder = mediaRecorder
    mediaRecorder = null

    const blob = await new Promise<Blob | null>((resolve) => {
      recorder.onstop = () => {
        if (!chunks.length) {
          resolve(null)
          return
        }
        resolve(new Blob(chunks, { type: recorder.mimeType || 'audio/webm' }))
      }
      if (recorder.state !== 'inactive') {
        recorder.stop()
      } else {
        resolve(chunks.length ? new Blob(chunks, { type: recorder.mimeType || 'audio/webm' }) : null)
      }
    })

    releaseStream()
    chunks = []
    return blob
  }

  function cancel() {
    clearTimer()
    recording.value = false
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
    mediaRecorder = null
    chunks = []
    releaseStream()
  }

  onUnmounted(cancel)

  return {
    recording,
    durationSec,
    durationLabel: () => formatRecordingDuration(durationSec.value),
    error,
    start,
    stop,
    cancel,
  }
}
