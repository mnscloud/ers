import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { apiClient, unwrap } from '../api/client';
import { DataTable, type DataTableColumn } from '../components/DataTable';
import type { ApiResponse, DataSource, IngestionBatch, PageResponse } from '../api/types';

const statusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  COMPLETED: 'success',
  PARTIAL: 'warning',
  FAILED: 'error',
  PENDING: 'default',
  PROCESSING: 'default',
};

const approvalStatusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  APPROVED: 'success', PENDING: 'warning', REJECTED: 'error',
};

async function fetchBatches(page: number, size: number) {
  return unwrap(
    apiClient.get<ApiResponse<PageResponse<IngestionBatch>>>(`/api/ingestion/batches?page=${page}&size=${size}`)
  );
}

export function IngestionPage() {
  const queryClient = useQueryClient();
  const [dataSourceId, setDataSourceId] = useState('');
  const [format, setFormat] = useState('CSV');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [dsPage, setDsPage] = useState(0);
  const [dsPageSize, setDsPageSize] = useState(20);

  const { data: dataSources } = useQuery({
    queryKey: ['data-sources'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<DataSource[]>>('/api/ingestion/data-sources')),
  });

  const invalidateDataSources = () => queryClient.invalidateQueries({ queryKey: ['data-sources'] });

  const approveDataSource = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/ingestion/data-sources/${id}/approve`, { comment: 'Reviewed and approved' }),
    onSuccess: invalidateDataSources,
    onError: () => setError('Approval failed - the maker cannot approve their own request'),
  });

  const rejectDataSource = useMutation({
    mutationFn: (id: string) => apiClient.post(`/api/ingestion/data-sources/${id}/reject`, { comment: 'Rejected' }),
    onSuccess: invalidateDataSources,
  });

  const dataSourceColumns: DataTableColumn<DataSource>[] = [
    { key: 'name', header: 'Name', render: (d) => d.name },
    { key: 'sourceSystem', header: 'Source System', render: (d) => d.sourceSystem },
    { key: 'type', header: 'Type', render: (d) => d.type },
    {
      key: 'approvalStatus',
      header: 'Status',
      render: (d) => <Chip size="small" label={d.approvalStatus} color={approvalStatusColor[d.approvalStatus] ?? 'default'} />,
      exportValue: (d) => d.approvalStatus,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'right',
      render: (d) =>
        d.approvalStatus === 'PENDING' ? (
          <Stack direction="row" spacing={1} justifyContent="flex-end">
            <Button size="small" color="success" onClick={() => approveDataSource.mutate(d.id)}>Approve</Button>
            <Button size="small" color="error" onClick={() => rejectDataSource.mutate(d.id)}>Reject</Button>
          </Stack>
        ) : null,
      exportValue: () => '',
    },
  ];

  const { data: batches, isLoading } = useQuery({
    queryKey: ['ingestion-batches', page, pageSize],
    queryFn: () => fetchBatches(page, pageSize),
    refetchInterval: 10_000,
  });

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!file || !dataSourceId) throw new Error('Select a data source and file');
      const form = new FormData();
      form.append('file', file);
      return unwrap(
        apiClient.post<ApiResponse<IngestionBatch>>(
          `/api/ingestion/upload?dataSourceId=${dataSourceId}&format=${format}`,
          form,
          { headers: { 'Content-Type': 'multipart/form-data' } }
        )
      );
    },
    onSuccess: () => {
      setFile(null);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['ingestion-batches'] });
    },
    onError: () => setError('Upload failed - check the file format and required columns'),
  });

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    uploadMutation.mutate();
  }

  const columns: DataTableColumn<IngestionBatch>[] = [
    { key: 'fileName', header: 'File', render: (b) => b.fileName },
    { key: 'sourceSystem', header: 'Source', render: (b) => b.dataSource?.sourceSystem, exportValue: (b) => b.dataSource?.sourceSystem ?? '' },
    {
      key: 'status',
      header: 'Status',
      render: (b) => <Chip size="small" label={b.status} color={statusColor[b.status] ?? 'default'} />,
      exportValue: (b) => b.status,
    },
    { key: 'totalRecords', header: 'Total', align: 'right', render: (b) => b.totalRecords },
    { key: 'successRecords', header: 'Success', align: 'right', render: (b) => b.successRecords },
    { key: 'failedRecords', header: 'Failed', align: 'right', render: (b) => b.failedRecords },
    { key: 'createdAt', header: 'Uploaded', render: (b) => new Date(b.createdAt).toLocaleString() },
  ];

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Data Ingestion
      </Typography>

      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Upload a source file
          </Typography>
          <Box component="form" onSubmit={handleSubmit}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
              <TextField
                select
                label="Data Source"
                value={dataSourceId}
                onChange={(e) => setDataSourceId(e.target.value)}
                sx={{ minWidth: 220 }}
                size="small"
              >
                {dataSources?.filter((ds) => ds.approvalStatus === 'APPROVED').map((ds) => (
                  <MenuItem key={ds.id} value={ds.id}>
                    {ds.name} ({ds.sourceSystem})
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label="Format"
                value={format}
                onChange={(e) => setFormat(e.target.value)}
                sx={{ minWidth: 120 }}
                size="small"
              >
                <MenuItem value="CSV">CSV</MenuItem>
                <MenuItem value="JSON">JSON</MenuItem>
                <MenuItem value="XLSX">XLSX</MenuItem>
                <MenuItem value="XML">XML</MenuItem>
              </TextField>
              <Button variant="outlined" component="label" size="small">
                {file ? file.name : 'Choose file'}
                <input type="file" hidden onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              </Button>
              <Button type="submit" variant="contained" disabled={uploadMutation.isPending || !file || !dataSourceId}>
                Upload
              </Button>
            </Stack>
            {error && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {error}
              </Alert>
            )}
          </Box>
        </CardContent>
      </Card>

      <Box sx={{ mb: 3 }}>
        <DataTable
          title="Data Sources"
          columns={dataSourceColumns}
          rows={(dataSources ?? []).slice(dsPage * dsPageSize, dsPage * dsPageSize + dsPageSize)}
          page={dsPage}
          pageSize={dsPageSize}
          totalElements={(dataSources ?? []).length}
          onPageChange={setDsPage}
          onPageSizeChange={(size) => {
            setDsPageSize(size);
            setDsPage(0);
          }}
          getRowId={(d) => d.id}
          fetchAllForExport={async () => dataSources ?? []}
          emptyMessage="No data sources yet."
        />
      </Box>

      <DataTable
        title="Ingestion Batches"
        columns={columns}
        rows={batches?.content ?? []}
        loading={isLoading}
        page={page}
        pageSize={pageSize}
        totalElements={batches?.totalElements ?? 0}
        onPageChange={setPage}
        onPageSizeChange={(size) => {
          setPageSize(size);
          setPage(0);
        }}
        getRowId={(b) => b.id}
        fetchAllForExport={async () => (await fetchBatches(0, 100_000)).content}
        emptyMessage="No batches uploaded yet."
      />
    </Box>
  );
}
