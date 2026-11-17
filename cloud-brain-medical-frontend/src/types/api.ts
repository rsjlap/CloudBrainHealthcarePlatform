export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface PageQuery {
  page?: number
  size?: number
}

export type UserRole = 'ADMIN' | 'DOCTOR' | 'PATIENT' | 'PHARMACIST' | 'INSPECTOR'

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  role: UserRole
  realName: string
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  phone: string
  role: UserRole
  avatarUrl?: string
}

export interface Department {
  id: number
  name: string
  code: string
  description?: string
  location?: string
}

export interface DoctorCard {
  id: number
  realName: string
  title: string
  specialty: string
  registrationFee: number
  departmentName: string
  scheduleSlots?: string[]
}

export interface DoctorProfile {
  userId: number
  username: string
  realName: string
  phone: string
  title?: string
  departmentName?: string
  specialty?: string
  introduction?: string
  registrationFee?: number
}

export interface Schedule {
  id: number
  doctorId: number
  workDate: string
  timeSlot: 'MORNING' | 'AFTERNOON' | 'EVENING'
  maxPatients: number
  registeredCount: number
  remainingSlots: number
}

export type RegistrationStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface Registration {
  id: number
  regNo: string
  patientId?: number
  patientName: string
  doctorName: string
  deptName: string
  workDate: string
  timeSlot: string
  status: RegistrationStatus
  queueNo: number
  chiefComplaint?: string
  createdAt: string
  paid?: boolean
}

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Drug {
  id: number
  drugCode: string
  drugName: string
  specification?: string
  unit?: string
  unitPrice?: number
  manufacturer?: string
  contraindication?: string
}

export interface PrescriptionItem {
  drugId?: number
  drugName: string
  drugCode?: string
  dosage?: number
  unit?: string
  frequency?: string
  durationDays?: number
  totalAmount?: number
  catalogMatched?: boolean
}

export interface PrescriptionItemRow extends PrescriptionItem {
  drugId?: number
}

export interface DrugInteraction {
  drug1: string
  drug2: string
  description: string
  severity: string
}

export interface PrescriptionReviewResult {
  riskLevel: RiskLevel
  overallAdvice: string
  inventoryAdvice?: string
  warnings?: string[]
  interactions: DrugInteraction[]
  aiAvailable: boolean
  kgAvailable?: boolean
  kgReference?: string
}

export interface RegistrationPatientInfo {
  registrationId: number
  patientId: number
  patientName: string
  regNo?: string
  workDate?: string
  queueNo?: number
  status?: RegistrationStatus
  gender?: number
  allergyHistory?: string
  medicalHistory?: string
}

export type PrescriptionStatus = 'PENDING_DISPENSE' | 'DISPENSED'

export interface Prescription {
  id: number
  registrationId: number
  patientName?: string
  doctorName: string
  status: PrescriptionStatus
  aiReviewResult?: string
  riskLevel?: RiskLevel
  dispensedByName?: string
  dispensedAt?: string
  paid?: boolean
  createdAt: string
  items: PrescriptionItem[]
}

export type MedicalRecordStatus = 'DRAFT' | 'CONFIRMED'

export interface AiMedicalDraft {
  chiefComplaint: string
  presentIllness: string
  pastHistory: string
  allergyHistory?: string
  personalHistory?: string
  maritalHistory?: string
  familyHistory?: string
  temperature?: string
  respiration?: string
  pulse?: string
  bloodPressure?: string
  physicalExam: string
  auxiliaryExam?: string
  diagnosis: string
  treatmentPlan: string
  recommendedExams?: RecommendedExam[]
  aiGenerated: boolean
  aiAvailable: boolean
  kgAvailable?: boolean
  kgReference?: string
}

export interface RecommendedExam {
  examType: string
  bodyPart?: string
  clinicalIndication?: string
  priority?: number
  orderType?: 'LAB' | 'EXAM'
  catalogItemId?: number
  itemCode?: string
  catalogMatched?: boolean
}

export interface ExamRecommendResult {
  recommendedExams: RecommendedExam[]
  summary?: string
  aiAvailable: boolean
  kgAvailable?: boolean
  kgReference?: string
}

export interface PrescriptionSuggestItem {
  drugId?: number
  drugName: string
  drugCode?: string
  dosage?: number
  unit?: string
  frequency?: string
  durationDays?: number
  reason?: string
  catalogMatched?: boolean
}

export interface PrescriptionSuggestResult {
  riskLevel: RiskLevel
  overallAdvice: string
  warnings?: string[]
  recommendedItems: PrescriptionSuggestItem[]
  interactions: DrugInteraction[]
  aiAvailable: boolean
}

export interface MedicalRecord {
  id: number
  registrationId: number
  patientName: string
  doctorName: string
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  dialogText?: string
  physicalExam?: string
  diagnosis?: string
  treatmentPlan?: string
  isAiGenerated?: number
  status: MedicalRecordStatus
  confirmedAt?: string
  createdAt?: string
}
