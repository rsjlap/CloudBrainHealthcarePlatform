export interface DoctorAlertMessage {
  type: string
  title: string
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH'
  message?: string
  overallAdvice?: string
  patientId?: number
  warnings?: string[]
  timestamp?: string
}

export const DOCTOR_ALERT_EVENT = 'doctor-ws-alert'
