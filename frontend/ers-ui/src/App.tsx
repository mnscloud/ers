import type { ReactNode } from 'react';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppLayout } from './layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { IngestionPage } from './pages/IngestionPage';
import { MasterDataPage } from './pages/MasterDataPage';
import { MatchingPage } from './pages/MatchingPage';
import { ReconciliationsPage } from './pages/ReconciliationsPage';
import { ExceptionsPage } from './pages/ExceptionsPage';
import { AdjustmentsPage } from './pages/AdjustmentsPage';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { AuditLogPage } from './pages/AuditLogPage';

function Shell({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute>
      <AppLayout>{children}</AppLayout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Shell><DashboardPage /></Shell>} />
      <Route path="/ingestion" element={<Shell><IngestionPage /></Shell>} />
      <Route path="/master-data" element={<Shell><MasterDataPage /></Shell>} />
      <Route path="/matching" element={<Shell><MatchingPage /></Shell>} />
      <Route path="/reconciliations" element={<Shell><ReconciliationsPage /></Shell>} />
      <Route path="/exceptions" element={<Shell><ExceptionsPage /></Shell>} />
      <Route path="/adjustments" element={<Shell><AdjustmentsPage /></Shell>} />
      <Route path="/admin" element={<Shell><AdminUsersPage /></Shell>} />
      <Route path="/audit" element={<Shell><AuditLogPage /></Shell>} />
    </Routes>
  );
}
