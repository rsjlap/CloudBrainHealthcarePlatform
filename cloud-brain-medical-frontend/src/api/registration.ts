import { get, put } from '@/utils/request'
import type { Registration, RegistrationPatientInfo } from '@/types/api'

export function doctorQueueApi(date?: string) {
  return get<Registration[]>('/registrations/queue', date ? { params: { date } } : undefined)
}

export function registrationPatientInfoApi(id: number) {
  return get<RegistrationPatientInfo>(`/registrations/${id}/patient-info`)
}

export function callRegistrationApi(id: number) {
  return put<Registration>(`/registrations/${id}/call`)
}

export function completeRegistrationApi(id: number) {
  return put<Registration>(`/registrations/${id}/complete`)
}
