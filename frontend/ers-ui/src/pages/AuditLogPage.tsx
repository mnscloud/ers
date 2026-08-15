import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Chip, MenuItem, TextField, Typography } from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, AuditEvent, PageResponse } from '../api/types';

const CATEGORIES = [
  { value: 'User', label: 'User (logins, password changes)' },
  { value: 'IngestionBatch', label: 'Data Ingestion' },
  { value: 'MatchRun', label: 'Matching' },
  { value: 'ReconciliationBreak', label: 'Exceptions' },
  { value: 'JournalEntry', label: 'Adjustments' },
  { value: 'AccountingPeriod', label: 'Period Lock' },
];

async function fetchAuditEvents(category: string, page: number, size: number) {
  return unwrap(
    apiClient.get<ApiResponse<PageResponse<AuditEvent>>>(
      `/api/compliance/audit-events?entityType=${category}&page=${page}&size=${size}`
    )
  );
}

export function AuditLogPage() {
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading } = useQuery({
    queryKey: ['audit-events', category, page, pageSize],
    queryFn: () => fetchAuditEvents(category, page, pageSize),
    enabled: category !== '',
    refetchInterval: category !== '' ? 15_000 : false,
  });

  const columns: DataTableColumn<AuditEvent>[] = [
    { key: 'occurredAt', header: 'When', render: (e) => new Date(e.occurredAt).toLocaleString() },
    { key: 'actor', header: 'Actor', render: (e) => e.actor },
    { key: 'action', header: 'Action', render: (e) => <Chip size="small" label={e.action} />, exportValue: (e) => e.action },
    { key: 'entityType', header: 'Entity', render: (e) => e.entityType },
    { key: 'summary', header: 'Summary', render: (e) => e.summary },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>Audit Trail</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Immutable log of security and business events. Entries cannot be edited or deleted.
      </Typography>
      <TextField
        select
        size="small"
        label="Category"
        value={category}
        onChange={(e) => {
          setCategory(e.target.value);
          setPage(0);
        }}
        sx={{ minWidth: 260, mb: 2 }}
      >
        <MenuItem value="">
          <em>Select a category</em>
        </MenuItem>
        {CATEGORIES.map((c) => (
          <MenuItem key={c.value} value={c.value}>
            {c.label}
          </MenuItem>
        ))}
      </TextField>
      <DataTable
        title="Audit Events"
        columns={columns}
        rows={data?.content ?? []}
        loading={category !== '' && isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={data?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(e) => e.id}
        fetchAllForExport={async () => (await fetchAuditEvents(category, 0, 100_000)).content}
        emptyMessage={category === '' ? 'Select a category above to view audit events.' : 'No audit events in this category.'}
      />
    </Box>
  );
}
