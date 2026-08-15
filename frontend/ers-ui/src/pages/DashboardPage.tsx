import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Grid2 as Grid, Typography, CircularProgress, Chip, Stack } from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import type { ApiResponse, DashboardSummary } from '../api/types';

function StatCard({ title, value, color }: { title: string; value: number; color?: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="body2" color="text.secondary">
          {title}
        </Typography>
        <Typography variant="h4" fontWeight={700} color={color}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

function StatusBreakdown({ title, data }: { title: string; data: Record<string, number> }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          {title}
        </Typography>
        <Stack direction="row" flexWrap="wrap" gap={1}>
          {Object.entries(data).map(([status, count]) => (
            <Chip key={status} label={`${status}: ${count}`} variant="outlined" />
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<DashboardSummary>>('/api/dashboard/summary')),
    refetchInterval: 30_000,
  });

  if (isLoading || !data) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
      </Box>
    );
  }

  const openBreaks = data.breaksByStatus.OPEN ?? 0;
  const pendingAdjustments = data.journalEntriesByStatus.PENDING_APPROVAL ?? 0;
  const completedRecons = data.reconciliationsByStatus.COMPLETED ?? 0;
  const postedAdjustments = data.journalEntriesByStatus.POSTED ?? 0;

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Operational Dashboard
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title="Open Exceptions" value={openBreaks} color="error.main" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title="Pending Approvals" value={pendingAdjustments} color="warning.main" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title="Completed Reconciliations" value={completedRecons} color="success.main" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard title="Posted Adjustments" value={postedAdjustments} />
        </Grid>
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <StatusBreakdown title="Reconciliations by Status" data={data.reconciliationsByStatus} />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <StatusBreakdown title="Exceptions by Status" data={data.breaksByStatus} />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <StatusBreakdown title="Journal Entries by Status" data={data.journalEntriesByStatus} />
        </Grid>
      </Grid>
    </Box>
  );
}
