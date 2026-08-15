import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid2 as Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, MatchRule, PageResponse, Reconciliation, ReconciliationTemplate } from '../api/types';

const statusColor: Record<string, 'success' | 'warning' | 'default' | 'info'> = {
  COMPLETED: 'success', IN_PROGRESS: 'warning', OPEN: 'default', LOCKED: 'info',
};

const approvalStatusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  APPROVED: 'success', PENDING: 'warning', REJECTED: 'error',
};

async function fetchReconciliations(page: number, size: number) {
  return unwrap(apiClient.get<ApiResponse<PageResponse<Reconciliation>>>(`/api/reconciliations?page=${page}&size=${size}`));
}

export function ReconciliationsPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [templateForm, setTemplateForm] = useState({ name: '', type: 'BANK_CASH', matchRuleId: '', owner: '' });
  const [reconForm, setReconForm] = useState({ templateId: '', periodCode: '' });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [templatesPage, setTemplatesPage] = useState(0);
  const [templatesPageSize, setTemplatesPageSize] = useState(20);

  const { data: templates } = useQuery({
    queryKey: ['recon-templates'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<ReconciliationTemplate[]>>('/api/reconciliation/templates')),
  });

  const invalidateTemplates = () => queryClient.invalidateQueries({ queryKey: ['recon-templates'] });

  const approveTemplate = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/reconciliation/templates/${id}/approve`, { comment: 'Reviewed and approved' }),
    onSuccess: invalidateTemplates,
    onError: () => setError('Approval failed - the maker cannot approve their own request'),
  });

  const rejectTemplate = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/reconciliation/templates/${id}/reject`, { comment: 'Rejected' }),
    onSuccess: invalidateTemplates,
  });

  const templateColumns: DataTableColumn<ReconciliationTemplate>[] = [
    { key: 'name', header: 'Name', render: (t) => t.name },
    { key: 'type', header: 'Type', render: (t) => t.type },
    { key: 'owner', header: 'Owner', render: (t) => t.owner },
    {
      key: 'approvalStatus',
      header: 'Status',
      render: (t) => <Chip size="small" label={t.approvalStatus} color={approvalStatusColor[t.approvalStatus] ?? 'default'} />,
      exportValue: (t) => t.approvalStatus,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (t) =>
        t.approvalStatus === 'PENDING' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" color="success" onClick={() => approveTemplate.mutate(t.id)}>Approve</Button>
            <Button size="small" color="error" onClick={() => rejectTemplate.mutate(t.id)}>Reject</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  const { data: rules } = useQuery({
    queryKey: ['match-rules'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<MatchRule[]>>('/api/matching/rules')),
  });

  const { data: reconciliations, isLoading } = useQuery({
    queryKey: ['reconciliations', page, pageSize],
    queryFn: () => fetchReconciliations(page, pageSize),
    refetchInterval: 10_000,
  });

  const createTemplate = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<ReconciliationTemplate>>('/api/reconciliation/templates', templateForm)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recon-templates'] });
      setTemplateForm({ name: '', type: 'BANK_CASH', matchRuleId: '', owner: '' });
    },
    onError: () => setError('Could not create template'),
  });

  const createRecon = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<Reconciliation>>('/api/reconciliations', reconForm)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reconciliations'] }),
    onError: () => setError('Could not create reconciliation'),
  });

  const triggerRecon = useMutation({
    mutationFn: (id: string) => unwrap(apiClient.post<ApiResponse<Reconciliation>>(`/api/reconciliations/${id}/trigger`)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reconciliations'] }),
    onError: () => setError('Could not trigger reconciliation - only Bank & Cash templates run automatically today'),
  });

  const columns: DataTableColumn<Reconciliation>[] = [
    { key: 'templateName', header: 'Template', render: (r) => r.template?.name, exportValue: (r) => r.template?.name ?? '' },
    { key: 'type', header: 'Type', render: (r) => r.type },
    { key: 'periodCode', header: 'Period', render: (r) => r.periodCode },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <Chip size="small" label={r.status} color={statusColor[r.status] ?? 'default'} />,
      exportValue: (r) => r.status,
    },
    { key: 'matchedCount', header: 'Matched', align: 'right', render: (r) => r.matchedCount },
    { key: 'unmatchedCount', header: 'Unmatched', align: 'right', render: (r) => r.unmatchedCount },
    {
      key: 'action',
      header: 'Action',
      align: 'right',
      render: (r) =>
        r.status === 'OPEN' ? (
          <Button size="small" onClick={() => triggerRecon.mutate(r.id)} disabled={triggerRecon.isPending}>
            Trigger
          </Button>
        ) : null,
      exportValue: () => '',
    },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>Reconciliation Management</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>New template</Typography>
              <Stack spacing={1.5}>
                <TextField size="small" label="Name" value={templateForm.name} onChange={(e) => setTemplateForm({ ...templateForm, name: e.target.value })} />
                <TextField size="small" select label="Type" value={templateForm.type} onChange={(e) => setTemplateForm({ ...templateForm, type: e.target.value })}>
                  <MenuItem value="BANK_CASH">Bank &amp; Cash</MenuItem>
                  <MenuItem value="GENERAL_LEDGER">General Ledger</MenuItem>
                  <MenuItem value="INTERCOMPANY">Intercompany</MenuItem>
                </TextField>
                <TextField size="small" select label="Match Rule" value={templateForm.matchRuleId} onChange={(e) => setTemplateForm({ ...templateForm, matchRuleId: e.target.value })}>
                  {rules?.map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)}
                </TextField>
                <TextField size="small" label="Owner" value={templateForm.owner} onChange={(e) => setTemplateForm({ ...templateForm, owner: e.target.value })} />
                <Button variant="contained" onClick={() => createTemplate.mutate()} disabled={createTemplate.isPending}>Create template</Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>New reconciliation</Typography>
              <Stack spacing={1.5}>
                <TextField size="small" select label="Template" value={reconForm.templateId} onChange={(e) => setReconForm({ ...reconForm, templateId: e.target.value })}>
                  {templates?.filter((t) => t.approvalStatus === 'APPROVED').map((t) => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
                </TextField>
                <TextField size="small" label="Period (YYYY-MM)" placeholder="2026-07" value={reconForm.periodCode} onChange={(e) => setReconForm({ ...reconForm, periodCode: e.target.value })} />
                <Button variant="contained" onClick={() => createRecon.mutate()} disabled={createRecon.isPending || !reconForm.templateId}>Create</Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Box sx={{ mb: 3 }}>
        <DataTable
          title="Templates"
          columns={templateColumns}
          rows={(templates ?? []).slice(templatesPage * templatesPageSize, templatesPage * templatesPageSize + templatesPageSize)}
          page={templatesPage}
          pageSize={templatesPageSize}
          totalElements={(templates ?? []).length}
          onPageChange={setTemplatesPage}
          onPageSizeChange={(size) => {
            setTemplatesPageSize(size);
            setTemplatesPage(0);
          }}
          getRowId={(t) => t.id}
          fetchAllForExport={async () => templates ?? []}
          emptyMessage="No templates yet."
        />
      </Box>

      <DataTable
        title="Reconciliations"
        columns={columns}
        rows={reconciliations?.content ?? []}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={reconciliations?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(r) => r.id}
        fetchAllForExport={async () => (await fetchReconciliations(0, 100_000)).content}
        emptyMessage="No reconciliations yet."
      />
    </Box>
  );
}
