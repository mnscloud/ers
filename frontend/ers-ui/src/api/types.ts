export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: unknown };
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
}

export interface CurrentUser {
  id: string | null;
  username: string;
  email: string | null;
  fullName: string;
  roles: string[];
  permissions: string[];
}

export interface DataSource {
  id: string;
  name: string;
  sourceSystem: string;
  type: string;
  defaultFormat: string;
  active: boolean;
  approvalStatus: string;
  approvalRequestId?: string;
}

export interface IngestionBatch {
  id: string;
  fileName: string;
  format: string;
  status: string;
  totalRecords: number;
  successRecords: number;
  failedRecords: number;
  dataSource: DataSource;
  createdAt: string;
}

export interface MatchRule {
  id: string;
  name: string;
  sourceSystemA: string;
  sourceSystemB: string;
  matchType: string;
  amountTolerance: number;
  dateToleranceDays: number;
  active: boolean;
  approvalStatus: string;
  approvalRequestId?: string;
}

export interface MatchRun {
  id: string;
  periodCode: string;
  status: string;
  matchedCount: number;
  unmatchedCountA: number;
  unmatchedCountB: number;
  createdAt: string;
}

export interface ReconciliationTemplate {
  id: string;
  name: string;
  type: string;
  owner: string;
  active: boolean;
  matchRule?: MatchRule;
  approvalStatus: string;
  approvalRequestId?: string;
}

export interface Reconciliation {
  id: string;
  type: string;
  periodCode: string;
  status: string;
  matchedCount: number;
  unmatchedCount: number;
  template: ReconciliationTemplate;
  createdAt: string;
}

export interface ReconciliationBreak {
  id: string;
  category: string;
  severity: string;
  status: string;
  assignee?: string;
  slaDueDate?: string;
  description: string;
  resolutionComment?: string;
  createdAt: string;
}

export interface JournalEntry {
  id: string;
  accountCode: string;
  debitCredit: string;
  amount: number;
  currency: string;
  description: string;
  periodCode: string;
  status: string;
  erpReference?: string;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  occurredAt: string;
  actor: string;
  action: string;
  entityType: string;
  entityId?: string;
  summary?: string;
}

export interface DashboardSummary {
  reconciliationsByStatus: Record<string, number>;
  breaksByStatus: Record<string, number>;
  journalEntriesByStatus: Record<string, number>;
}

export interface MasterDataItem {
  id: string;
  code: string;
  name: string;
  description?: string;
  active: boolean;
  approvalStatus: string;
  approvalRequestId?: string;
}

export interface UserAccount {
  id: string;
  username: string;
  email: string;
  fullName: string;
  enabled: boolean;
  accountLocked: boolean;
  roles: string[];
}
