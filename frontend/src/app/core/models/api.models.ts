/** Contratos da API do Educa Mais (espelham os DTOs do backend). */

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: 'ALUNO' | 'PROFESSOR' | 'ADMIN';
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone?: string;
  birthDate?: string;
  password: string;
  interests: string[];
  whatsAppOptIn: boolean;
  schoolId?: string;
  classroomId?: string;
  guardianConsent?: 'ACEITE' | 'NAO_ACEITE';
}

export interface Profile {
  id: string;
  name: string;
  email: string;
  phone?: string;
  status: string;
  interests: string[];
  whatsAppOptIn: boolean;
}

export interface Gamification {
  studentId: string;
  totalXp: number;
  level: number;
  xpToNextLevel: number;
  diamonds: number;
  coins: number;
  currentStreak: number;
  longestStreak: number;
}

export interface MissionStep {
  order: number;
  objective: string;
  explanation: string;
  example: string;
  strategy: 'EXPLICIT' | 'ANALOGY' | 'VISUAL' | 'STEP_BY_STEP';
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'COMPLETED_WITH_SUPPORT' | 'NEEDS_REINFORCEMENT';
  attempts: number;
}

export interface Question {
  id: string;
  prompt: string;
  type: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER';
  options: string[];
  difficulty: number;
}

export interface Mission {
  id: string;
  status: string;
  planSource: 'LLM' | 'FALLBACK';
  progress: number;
  attempts: number;
  finished: boolean;
  createdAt: string;
  completedAt?: string;
  steps: MissionStep[];
  question?: Question;
}

export interface AnswerResponse {
  classification: string;
  feedback: string;
  mission: Mission;
}

export interface NotificationItem {
  id: string;
  channel: 'IN_APP' | 'WHATSAPP';
  status: 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';
  payload: string;
  failureReason?: string;
  sentAt?: string;
}

export interface ShopItem {
  code: string;
  name: string;
  category: string;
  priceCoins: number;
  assetUrl?: string;
  owned: boolean;
}

export interface TeacherReport {
  id: string;
  missionId: string;
  studentId: string;
  summary: string;
  identifiedGaps: string[];
  recommendation: string;
  successRate: number;
  deliveryStatus: string;
  createdAt: string;
}

export interface Dashboard {
  classroomId: string;
  totalStudents: number;
  pendingMissions: number;
  completedMissions: number;
  completionRate: number;
  reachableByWhatsApp: number;
  students: {
    studentId: string;
    firstName: string;
    status: string;
    pendingMissions: number;
    completedMissions: number;
    stepsNeedingSupport: number;
    reportsAvailable: number;
  }[];
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  violations?: { field: string; message: string }[];
}
