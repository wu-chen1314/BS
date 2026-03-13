export interface QuizQuestion {
  id: number;
  question: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  category?: string | null;
  difficulty?: string | null;
}

export interface QuizAnswerResult {
  questionId: number;
  question: string;
  selectedIndex: number | null;
  correctIndex: number;
  correct: boolean;
  category?: string | null;
  explanation: string;
}

export interface QuizAttemptResult {
  attemptId?: number;
  score: number;
  correctCount: number;
  totalQuestions: number;
  durationSeconds: number;
  submittedAt: string;
  answerResults: QuizAnswerResult[];
}

export interface QuizHistoryItem {
  id: number;
  score: number;
  correctCount: number;
  totalQuestions: number;
  durationSeconds: number;
  createdAt: string;
}

export interface QuizSummary {
  attemptCount: number;
  bestScore: number;
  averageScore: number;
  latestScore: number | null;
  latestCorrectCount: number | null;
  latestAttemptAt: string | null;
  consistency: string;
}

export interface QuizAttemptAnswer {
  questionId: number;
  selectedIndex: number | null;
}

export interface QuizQuestionRecord extends QuizQuestion {
  sortOrder: number;
  active: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface QuizQuestionAdminPage {
  records: QuizQuestionRecord[];
  total: number;
  current: number;
  size: number;
}

export interface QuizQuestionEditorPayload {
  question: string;
  options: string[];
  correctAnswer: number;
  explanation: string;
  category?: string;
  difficulty?: string;
  sortOrder?: number;
  active?: boolean;
}
