import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid2 as Grid, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, MasterDataItem } from '../api/types';

const statusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  APPROVED: 'success', PENDING: 'warning', REJECTED: 'error',
};

interface MasterDataTabProps {
  title: string;
  apiPath: string;
  queryKey: string;
}

function MasterDataTab({ title, apiPath, queryKey }: MasterDataTabProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ code: '', name: '', description: '' });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading } = useQuery({
    queryKey: [queryKey],
    queryFn: () => unwrap(apiClient.get<ApiResponse<MasterDataItem[]>>(apiPath)),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: [queryKey] });

  const createMutation = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<MasterDataItem>>(apiPath, form)),
    onSuccess: () => {
      invalidate();
      setForm({ code: '', name: '', description: '' });
      setError(null);
    },
    onError: () => setError('Could not create - check the code is unique'),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => apiClient.post(`${apiPath}/${id}/approve`, { comment: 'Reviewed and approved' }),
    onSuccess: invalidate,
    onError: () => setError('Approval failed - the maker cannot approve their own request'),
  });

  const rejectMutation = useMutation({
    mutationFn: (id: string) => apiClient.post(`${apiPath}/${id}/reject`, { comment: 'Rejected' }),
    onSuccess: invalidate,
  });

  const rows = data ?? [];
  const pagedRows = rows.slice(page * pageSize, page * pageSize + pageSize);

  const columns: DataTableColumn<MasterDataItem>[] = [
    { key: 'code', header: 'Code', render: (r) => r.code },
    { key: 'name', header: 'Name', render: (r) => r.name },
    { key: 'description', header: 'Description', render: (r) => r.description ?? '-' },
    {
      key: 'approvalStatus',
      header: 'Status',
      render: (r) => <Chip size="small" label={r.approvalStatus} color={statusColor[r.approvalStatus] ?? 'default'} />,
      exportValue: (r) => r.approvalStatus,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (r) =>
        r.approvalStatus === 'PENDING' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" color="success" onClick={() => approveMutation.mutate(r.id)}>Approve</Button>
            <Button size="small" color="error" onClick={() => rejectMutation.mutate(r.id)}>Reject</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  return (
    <Box>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>New {title.toLowerCase()}</Typography>
          <Grid container spacing={1.5}>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField size="small" fullWidth label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField size="small" fullWidth label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 3 }}>
              <TextField size="small" fullWidth label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 2 }}>
              <Button fullWidth variant="contained" onClick={() => createMutation.mutate()} disabled={createMutation.isPending || !form.code || !form.name}>
                Create
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <DataTable
        title={title}
        columns={columns}
        rows={pagedRows}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={rows.length}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(r) => r.id}
        fetchAllForExport={async () => rows}
        emptyMessage="No records yet."
      />
    </Box>
  );
}

const TABS = [
  { label: 'Transaction Types', apiPath: '/api/masterdata/transaction-types', queryKey: 'transaction-types' },
  { label: 'GL Accounts', apiPath: '/api/masterdata/gl-accounts', queryKey: 'gl-accounts' },
  { label: 'Currencies', apiPath: '/api/masterdata/currencies', queryKey: 'currencies' },
  { label: 'Counterparties', apiPath: '/api/masterdata/counterparties', queryKey: 'counterparties' },
];

export function MasterDataPage() {
  const [tab, setTab] = useState(0);
  const active = TABS[tab];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>Master Data</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Reference data used across reconciliation. New entries require approval by a different user (two-eyes control) before they become active.
      </Typography>
      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ mb: 2 }}>
        {TABS.map((t) => <Tab key={t.queryKey} label={t.label} />)}
      </Tabs>
      <MasterDataTab key={active.queryKey} title={active.label} apiPath={active.apiPath} queryKey={active.queryKey} />
    </Box>
  );
}
