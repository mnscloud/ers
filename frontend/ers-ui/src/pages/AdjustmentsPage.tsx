import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid2 as Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, JournalEntry, PageResponse } from '../api/types';

const statusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  POSTED: 'success', PENDING_APPROVAL: 'warning', REJECTED: 'error', DRAFT: 'default', APPROVED: 'success',
};

async function fetchJournalEntries(page: number, size: number) {
  return unwrap(apiClient.get<ApiResponse<PageResponse<JournalEntry>>>(`/api/adjustments?page=${page}&size=${size}`));
}

export function AdjustmentsPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ accountCode: '', debitCredit: 'DEBIT', amount: '', currency: 'USD', description: '', periodCode: '' });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading } = useQuery({
    queryKey: ['journal-entries', page, pageSize],
    queryFn: () => fetchJournalEntries(page, pageSize),
    refetchInterval: 10_000,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['journal-entries'] });

  const createMutation = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<JournalEntry>>('/api/adjustments', { ...form, amount: Number(form.amount) })),
    onSuccess: () => {
      invalidate();
      setForm({ accountCode: '', debitCredit: 'DEBIT', amount: '', currency: 'USD', description: '', periodCode: '' });
      setError(null);
    },
    onError: () => setError('Could not create journal entry - check the period is not locked'),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/adjustments/${id}/approve`, { comment: 'Reviewed and approved' }),
    onSuccess: invalidate,
    onError: () => setError('Approval failed - the maker cannot approve their own request'),
  });

  const rejectMutation = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/adjustments/${id}/reject`, { comment: 'Rejected' }),
    onSuccess: invalidate,
  });

  const columns: DataTableColumn<JournalEntry>[] = [
    { key: 'accountCode', header: 'Account', render: (j) => j.accountCode },
    { key: 'debitCredit', header: 'Dr/Cr', render: (j) => j.debitCredit },
    { key: 'amount', header: 'Amount', align: 'right', render: (j) => `${j.currency} ${j.amount.toFixed(2)}`, exportValue: (j) => j.amount },
    { key: 'periodCode', header: 'Period', render: (j) => j.periodCode },
    {
      key: 'status',
      header: 'Status',
      render: (j) => <Chip size="small" label={j.status} color={statusColor[j.status] ?? 'default'} />,
      exportValue: (j) => j.status,
    },
    { key: 'erpReference', header: 'ERP Ref', render: (j) => j.erpReference ?? '-' },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (j) =>
        j.status === 'PENDING_APPROVAL' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" color="success" onClick={() => approveMutation.mutate(j.id)}>Approve</Button>
            <Button size="small" color="error" onClick={() => rejectMutation.mutate(j.id)}>Reject</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>Adjustment &amp; Journal Posting</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>New adjustment</Typography>
          <Grid container spacing={1.5}>
            <Grid size={{ xs: 12, sm: 4, md: 2 }}>
              <TextField size="small" fullWidth label="Account code" value={form.accountCode} onChange={(e) => setForm({ ...form, accountCode: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4, md: 2 }}>
              <TextField size="small" fullWidth select label="Debit / Credit" value={form.debitCredit} onChange={(e) => setForm({ ...form, debitCredit: e.target.value })}>
                <MenuItem value="DEBIT">Debit</MenuItem>
                <MenuItem value="CREDIT">Credit</MenuItem>
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 4, md: 2 }}>
              <TextField size="small" fullWidth label="Amount" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4, md: 2 }}>
              <TextField size="small" fullWidth label="Currency" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4, md: 2 }}>
              <TextField size="small" fullWidth label="Period (YYYY-MM)" placeholder="2026-07" value={form.periodCode} onChange={(e) => setForm({ ...form, periodCode: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 8, md: 2 }}>
              <Button fullWidth variant="contained" onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>Create</Button>
            </Grid>
            <Grid size={12}>
              <TextField size="small" fullWidth label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <DataTable
        title="Journal Entries"
        columns={columns}
        rows={data?.content ?? []}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={data?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(j) => j.id}
        fetchAllForExport={async () => (await fetchJournalEntries(0, 100_000)).content}
        emptyMessage="No journal entries yet."
      />
    </Box>
  );
}
