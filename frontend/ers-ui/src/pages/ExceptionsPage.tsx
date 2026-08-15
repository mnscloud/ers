import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, PageResponse, ReconciliationBreak } from '../api/types';

const severityColor: Record<string, 'error' | 'warning' | 'info' | 'default'> = {
  CRITICAL: 'error', HIGH: 'error', MEDIUM: 'warning', LOW: 'info',
};

async function fetchExceptions(status: string, page: number, size: number) {
  return unwrap(
    apiClient.get<ApiResponse<PageResponse<ReconciliationBreak>>>(
      `/api/exceptions?status=${status}&page=${page}&size=${size}`
    )
  );
}

export function ExceptionsPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('OPEN');
  const [assigneeDialog, setAssigneeDialog] = useState<string | null>(null);
  const [resolveDialog, setResolveDialog] = useState<string | null>(null);
  const [assignee, setAssignee] = useState('');
  const [resolutionComment, setResolutionComment] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading } = useQuery({
    queryKey: ['exceptions', status, page, pageSize],
    queryFn: () => fetchExceptions(status, page, pageSize),
    refetchInterval: 15_000,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['exceptions'] });

  const assignMutation = useMutation({
    mutationFn: (id: string) => apiClient.patch(`/api/exceptions/${id}/assign`, { assignee }),
    onSuccess: () => { invalidate(); setAssigneeDialog(null); setAssignee(''); },
  });

  const escalateMutation = useMutation({
    mutationFn: (id: string) => apiClient.patch(`/api/exceptions/${id}/escalate`),
    onSuccess: invalidate,
  });

  const resolveMutation = useMutation({
    mutationFn: (id: string) => apiClient.patch(`/api/exceptions/${id}/resolve`, { resolutionComment }),
    onSuccess: () => { invalidate(); setResolveDialog(null); setResolutionComment(''); },
  });

  const columns: DataTableColumn<ReconciliationBreak>[] = [
    { key: 'description', header: 'Description', render: (b) => b.description },
    { key: 'category', header: 'Category', render: (b) => b.category },
    {
      key: 'severity',
      header: 'Severity',
      render: (b) => <Chip size="small" label={b.severity} color={severityColor[b.severity] ?? 'default'} />,
      exportValue: (b) => b.severity,
    },
    { key: 'assignee', header: 'Assignee', render: (b) => b.assignee ?? '-' },
    { key: 'slaDueDate', header: 'SLA Due', render: (b) => b.slaDueDate ?? '-' },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (b) =>
        b.status !== 'RESOLVED' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" onClick={() => setAssigneeDialog(b.id)}>Assign</Button>
            <Button size="small" onClick={() => escalateMutation.mutate(b.id)}>Escalate</Button>
            <Button size="small" color="success" onClick={() => setResolveDialog(b.id)}>Resolve</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" fontWeight={600}>Exception Triage</Typography>
        <TextField
          select
          size="small"
          label="Status"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="OPEN">Open</MenuItem>
          <MenuItem value="IN_REVIEW">In Review</MenuItem>
          <MenuItem value="ESCALATED">Escalated</MenuItem>
          <MenuItem value="RESOLVED">Resolved</MenuItem>
        </TextField>
      </Stack>

      <DataTable
        title="Exceptions"
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
        getRowId={(b) => b.id}
        fetchAllForExport={async () => (await fetchExceptions(status, 0, 100_000)).content}
        emptyMessage="No exceptions in this status."
      />

      <Dialog open={Boolean(assigneeDialog)} onClose={() => setAssigneeDialog(null)}>
        <DialogTitle>Assign exception</DialogTitle>
        <DialogContent>
          <TextField autoFocus label="Assignee username" fullWidth margin="dense" value={assignee} onChange={(e) => setAssignee(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssigneeDialog(null)}>Cancel</Button>
          <Button variant="contained" onClick={() => assigneeDialog && assignMutation.mutate(assigneeDialog)}>Assign</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(resolveDialog)} onClose={() => setResolveDialog(null)}>
        <DialogTitle>Resolve exception</DialogTitle>
        <DialogContent>
          <TextField autoFocus label="Resolution comment" fullWidth multiline rows={3} margin="dense" value={resolutionComment} onChange={(e) => setResolutionComment(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveDialog(null)}>Cancel</Button>
          <Button variant="contained" color="success" onClick={() => resolveDialog && resolveMutation.mutate(resolveDialog)}>Resolve</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
