import { onMounted, onUnmounted, ref } from 'vue'
import { ElNotification } from 'element-plus'
import type { DoctorAlertMessage } from '@/types/websocket'
import { DOCTOR_ALERT_EVENT } from '@/types/websocket'

const TOKEN_KEY = 'cbm_token'
const RECONNECT_MS = 4000
const PING_MS = 30000

function buildWebSocketUrl(token: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/api/v1/ws/doctor?token=${encodeURIComponent(token)}`
}

function showHighRiskAlert(alert: DoctorAlertMessage) {
  const advice = alert.overallAdvice?.trim()
  const warnings = alert.warnings?.filter(Boolean) ?? []
  const detailParts = [advice, ...warnings].filter(Boolean)
  ElNotification({
    title: alert.title || '高风险用药告警',
    message: detailParts.join('\n') || alert.message || 'AI 处方审核发现高风险用药，请立即关注',
    type: 'error',
    duration: 0,
    position: 'top-right',
  })
  window.dispatchEvent(new CustomEvent(DOCTOR_ALERT_EVENT, { detail: alert }))
}

function handleAlertPayload(payload: string) {
  if (payload === 'pong') return
  try {
    const alert = JSON.parse(payload) as DoctorAlertMessage
    if (alert.type === 'PRESCRIPTION_HIGH_RISK' || alert.riskLevel === 'HIGH') {
      showHighRiskAlert(alert)
    }
  } catch {
    // ignore non-json frames
  }
}

export function useDoctorWebSocket() {
  const connected = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: number | undefined
  let pingTimer: number | undefined
  let manualClose = false

  function clearReconnectTimer() {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = undefined
    }
  }

  function clearPingTimer() {
    if (pingTimer) {
      window.clearInterval(pingTimer)
      pingTimer = undefined
    }
  }

  function scheduleReconnect() {
    if (manualClose) return
    clearReconnectTimer()
    reconnectTimer = window.setTimeout(() => {
      connect()
    }, RECONNECT_MS)
  }

  function startPing() {
    clearPingTimer()
    pingTimer = window.setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) {
        ws.send('ping')
      }
    }, PING_MS)
  }

  function connect() {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      connected.value = false
      return
    }

    manualClose = false
    clearReconnectTimer()

    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    ws = new WebSocket(buildWebSocketUrl(token))

    ws.onopen = () => {
      connected.value = true
      startPing()
    }

    ws.onmessage = (event) => {
      handleAlertPayload(String(event.data))
    }

    ws.onclose = () => {
      connected.value = false
      clearPingTimer()
      ws = null
      scheduleReconnect()
    }

    ws.onerror = () => {
      connected.value = false
    }
  }

  function disconnect() {
    manualClose = true
    clearReconnectTimer()
    clearPingTimer()
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return {
    connected,
    connect,
    disconnect,
  }
}
