import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid2 as Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, MatchRule, MatchRun, PageResponse } from '../api/types';

async function fetchRuns(page: number, size: number) {
  return unwrap(apiClient.get<ApiResponse<PageResponse<MatchRun>>>(`/api/matching/runs?page=${page}&size=${size}`));
}

const approvalStatusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  APPROVED: 'success', PENDING: 'warning', REJECTED: 'error',
};

export function MatchingPage() {
  const queryClient = useQueryClient();
  const [ruleForm, setRuleForm] = useState({
    name: '', sourceSystemA: '', sourceSystemB: '', matchType: 'ONE_TO_ONE', amountTolerance: '0.01', dateToleranceDays: '2',
  });
  const [runForm, setRunForm] = useState({ matchRuleId: '', periodCode: '' });
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [rulesPage, setRulesPage] = useState(0);
  const [rulesPageSize, setRulesPageSize] = useState(20);

  const { data: rules } = useQuery({
    queryKey: ['match-rules'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<MatchRule[]>>('/api/matching/rules')),
  });

  const invalidateRules = () => queryClient.invalidateQueries({ queryKey: ['match-rules'] });

  const approveRule = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/matching/rules/${id}/approve`, { comment: 'Reviewed and approved' }),
    onSuccess: invalidateRules,
    onError: () => setError('Approval failed - the maker cannot approve their own request'),
  });

  const rejectRule = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/matching/rules/${id}/reject`, { comment: 'Rejected' }),
    onSuccess: invalidateRules,
  });

  const ruleColumns: DataTableColumn<MatchRule>[] = [
    { key: 'name', header: 'Name', render: (r) => r.name },
    { key: 'sourceSystemA', header: 'Source A', render: (r) => r.sourceSystemA },
    { key: 'sourceSystemB', header: 'Source B', render: (r) => r.sourceSystemB },
    { key: 'matchType', header: 'Match Type', render: (r) => r.matchType },
    {
      key: 'approvalStatus',
      header: 'Status',
      render: (r) => <Chip size="small" label={r.approvalStatus} color={approvalStatusColor[r.approvalStatus] ?? 'default'} />,
      exportValue: (r) => r.approvalStatus,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (r) =>
        r.approvalStatus === 'PENDING' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" color="success" onClick={() => approveRule.mutate(r.id)}>Approve</Button>
            <Button size="small" color="error" onClick={() => rejectRule.mutate(r.id)}>Reject</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  const { data: runs, isLoading } = useQuery({
    queryKey: ['match-runs', page, pageSize],
    queryFn: () => fetchRuns(page, pageSize),
    refetchInterval: 10_000,
  });

  const createRule = useMutation({
    mutationFn: () =>
      unwrap(
        apiClient.post<ApiResponse<MatchRule>>('/api/matching/rules', {
          ...ruleForm,
          amountTolerance: Number(ruleForm.amountTolerance),
          dateToleranceDays: Number(ruleForm.dateToleranceDays),
        })
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['match-rules'] });
      setRuleForm({ name: '', sourceSystemA: '', sourceSystemB: '', matchType: 'ONE_TO_ONE', amountTolerance: '0.01', dateToleranceDays: '2' });
    },
    onError: () => setError('Could not create match rule'),
  });

  const triggerRun = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<MatchRun>>('/api/matching/runs', runForm)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['match-runs'] });
      setError(null);
    },
    onError: () => setError('Could not run matching - check the rule and period code (YYYY-MM)'),
  });

  const columns: DataTableColumn<MatchRun>[] = [
    { key: 'periodCode', header: 'Period', render: (r) => r.periodCode },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <Chip size="small" label={r.status} color={r.status === 'COMPLETED' ? 'success' : 'default'} />,
      exportValue: (r) => r.status,
    },
    { key: 'matchedCount', header: 'Matched', align: 'right', render: (r) => r.matchedCount },
    { key: 'unmatchedCountA', header: 'Unmatched A', align: 'right', render: (r) => r.unmatchedCountA },
    { key: 'unmatchedCountB', header: 'Unmatched B', align: 'right', render: (r) => r.unmatchedCountB },
    { key: 'createdAt', header: 'Run at', render: (r) => new Date(r.createdAt).toLocaleString() },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Transaction Matching Engine
      </Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>New match rule</Typography>
              <Stack spacing={1.5}>
                <TextField size="small" label="Name" value={ruleForm.name} onChange={(e) => setRuleForm({ ...ruleForm, name: e.target.value })} />
                <TextField size="small" label="Source System A" value={ruleForm.sourceSystemA} onChange={(e) => setRuleForm({ ...ruleForm, sourceSystemA: e.target.value })} />
                <TextField size="small" label="Source System B" value={ruleForm.sourceSystemB} onChange={(e) => setRuleForm({ ...ruleForm, sourceSystemB: e.target.value })} />
                <TextField size="small" select label="Match Type" value={ruleForm.matchType} onChange={(e) => setRuleForm({ ...ruleForm, matchType: e.target.value })}>
                  <MenuItem value="ONE_TO_ONE">One to one</MenuItem>
                  <MenuItem value="ONE_TO_MANY">One to many</MenuItem>
                  <MenuItem value="MANY_TO_MANY">Many to many</MenuItem>
                </TextField>
                <Stack direction="row" spacing={1.5}>
                  <TextField size="small" label="Amount tolerance" value={ruleForm.amountTolerance} onChange={(e) => setRuleForm({ ...ruleForm, amountTolerance: e.target.value })} />
                  <TextField size="small" label="Date tolerance (days)" value={ruleForm.dateToleranceDays} onChange={(e) => setRuleForm({ ...ruleForm, dateToleranceDays: e.target.value })} />
                </Stack>
                <Button variant="contained" onClick={() => createRule.mutate()} disabled={createRule.isPending}>
                  Create rule
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>Run matching</Typography>
              <Stack spacing={1.5}>
                <TextField size="small" select label="Match Rule" value={runForm.matchRuleId} onChange={(e) => setRunForm({ ...runForm, matchRuleId: e.target.value })}>
                  {rules?.filter((r) => r.approvalStatus === 'APPROVED').map((r) => (
                    <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
                  ))}
                </TextField>
                <TextField size="small" label="Period (YYYY-MM)" placeholder="2026-07" value={runForm.periodCode} onChange={(e) => setRunForm({ ...runForm, periodCode: e.target.value })} />
                <Button variant="contained" onClick={() => triggerRun.mutate()} disabled={triggerRun.isPending || !runForm.matchRuleId}>
                  Run
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Box sx={{ mb: 3 }}>
        <DataTable
          title="Match Rules"
          columns={ruleColumns}
          rows={(rules ?? []).slice(rulesPage * rulesPageSize, rulesPage * rulesPageSize + rulesPageSize)}
          page={rulesPage}
          pageSize={rulesPageSize}
          totalElements={(rules ?? []).length}
          onPageChange={setRulesPage}
          onPageSizeChange={(size) => {
            setRulesPageSize(size);
            setRulesPage(0);
          }}
          getRowId={(r) => r.id}
          fetchAllForExport={async () => rules ?? []}
          emptyMessage="No match rules yet."
        />
      </Box>

      <DataTable
        title="Match Runs"
        columns={columns}
        rows={runs?.content ?? []}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={runs?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(r) => r.id}
        fetchAllForExport={async () => (await fetchRuns(0, 100_000)).content}
        emptyMessage="No match runs yet."
      />
    </Box>
  );
}
