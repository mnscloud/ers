import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Box, Button, Card, CardContent, Chip, Grid2 as Grid, MenuItem, Select, Stack, TextField, Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, PageResponse, UserAccount } from '../api/types';

const AVAILABLE_ROLES = ['ADMIN', 'RECON_MAKER', 'RECON_CHECKER', 'COMPLIANCE', 'VIEWER'];

async function fetchUsers(page: number, size: number) {
  return unwrap(apiClient.get<ApiResponse<PageResponse<UserAccount>>>(`/api/admin/users?page=${page}&size=${size}`));
}

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ username: '', email: '', fullName: '', password: '', roles: ['VIEWER'] as string[] });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin-users', page, pageSize],
    queryFn: () => fetchUsers(page, pageSize),
  });

  const createMutation = useMutation({
    mutationFn: () => unwrap(apiClient.post<ApiResponse<UserAccount>>('/api/admin/users', form)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setForm({ username: '', email: '', fullName: '', password: '', roles: ['VIEWER'] });
      setError(null);
    },
    onError: () => setError('Could not create user - username/email may already be taken'),
  });

  const toggleEnabled = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      apiClient.patch(`/api/admin/users/${id}/enabled?enabled=${enabled}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  const columns: DataTableColumn<UserAccount>[] = [
    { key: 'username', header: 'Username', render: (u) => u.username },
    { key: 'email', header: 'Email', render: (u) => u.email },
    {
      key: 'roles',
      header: 'Roles',
      render: (u) => (
        <Stack direction="row" spacing={0.5} flexWrap="wrap">
          {u.roles.map((r) => <Chip key={r} size="small" label={r} />)}
        </Stack>
      ),
      exportValue: (u) => u.roles.join(', '),
    },
    { key: 'enabled', header: 'Enabled', render: (u) => (u.enabled ? 'Yes' : 'No') },
    { key: 'accountLocked', header: 'Locked', render: (u) => (u.accountLocked ? 'Yes' : 'No') },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (u) => (
        <Button size="small" onClick={() => toggleEnabled.mutate({ id: u.id, enabled: !u.enabled })}>
          {u.enabled ? 'Disable' : 'Enable'}
        </Button>
      ),
      exportValue: () => '',
    },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>User &amp; Role Administration</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>New user</Typography>
          <Grid container spacing={1.5}>
            <Grid size={{ xs: 12, sm: 6, md: 2 }}>
              <TextField size="small" fullWidth label="Username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField size="small" fullWidth label="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 2 }}>
              <TextField size="small" fullWidth label="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 2 }}>
              <TextField size="small" fullWidth type="password" label="Password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </Grid>
            <Grid size={{ xs: 12, sm: 8, md: 2 }}>
              <Select
                size="small"
                fullWidth
                multiple
                value={form.roles}
                onChange={(e) => setForm({ ...form, roles: typeof e.target.value === 'string' ? [e.target.value] : e.target.value })}
                renderValue={(selected) => selected.join(', ')}
              >
                {AVAILABLE_ROLES.map((role) => <MenuItem key={role} value={role}>{role}</MenuItem>)}
              </Select>
            </Grid>
            <Grid size={{ xs: 12, sm: 4, md: 1 }}>
              <Button fullWidth variant="contained" onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>Create</Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <DataTable
        title="Users"
        columns={columns}
        rows={users?.content ?? []}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={users?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(u) => u.id}
        fetchAllForExport={async () => (await fetchUsers(0, 100_000)).content}
      />
    </Box>
  );
}
