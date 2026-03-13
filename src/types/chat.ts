export interface ChatSessionRecord {
  id: number;
  title?: string | null;
  lastMessage?: string | null;
  updatedAt?: string | null;
}

export interface ChatHistoryRecord {
  role: "user" | "assistant";
  content: string;
  timestamp?: string | null;
}

export interface ChatMessage {
  id: number;
  role: "user" | "assistant" | "system";
  content: string;
  timestamp: Date | string;
  status?: "sending" | "sent" | "error";
  isThinking?: boolean;
}
