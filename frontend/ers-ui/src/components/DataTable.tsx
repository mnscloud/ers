import { useState, type ReactNode } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Menu,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Toolbar,
  Typography,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import { exportToCsv, exportToPdf, exportToXlsx, type ExportColumn } from '../lib/export';

export interface DataTableColumn<T> {
  key: string;
  header: string;
  align?: 'left' | 'right' | 'center';
  render: (row: T) => ReactNode;
  /** Only needed when `render` doesn't return a plain string/number (e.g. a Chip) - otherwise
   * the raw `row[key]` value is used for CSV/XLSX/PDF export. */
  exportValue?: (row: T) => string | number;
}

interface DataTableProps<T> {
  title: string;
  columns: DataTableColumn<T>[];
  rows: T[];
  loading?: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  getRowId: (row: T) => string;
  /** Re-fetches the full filtered result set (not just the current page) for export. */
  fetchAllForExport?: () => Promise<T[]>;
  emptyMessage?: string;
}

function defaultExportValue(row: unknown, key: string): string {
  const value = (row as Record<string, unknown>)[key];
  if (value === null || value === undefined) return '';
  return String(value);
}

export function DataTable<T>({
  title,
  columns,
  rows,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  getRowId,
  fetchAllForExport,
  emptyMessage = 'No rows.',
}: DataTableProps<T>) {
  const [exportAnchor, setExportAnchor] = useState<null | HTMLElement>(null);
  const [exporting, setExporting] = useState(false);

  const exportColumns: ExportColumn<T>[] = [
    { header: '#', value: (_row, index) => index + 1 },
    ...columns.map((c) => ({
      header: c.header,
      value: (row: T) => (c.exportValue ? c.exportValue(row) : defaultExportValue(row, c.key)),
    })),
  ];

  async function handleExport(format: 'csv' | 'xlsx' | 'pdf') {
    setExportAnchor(null);
    setExporting(true);
    try {
      const exportRows = fetchAllForExport ? await fetchAllForExport() : rows;
      const filenameBase = title.toLowerCase().replace(/[^a-z0-9]+/g, '-');
      if (format === 'csv') {
        exportToCsv(`${filenameBase}.csv`, exportColumns, exportRows);
      } else if (format === 'xlsx') {
        await exportToXlsx(`${filenameBase}.xlsx`, title.slice(0, 31), exportColumns, exportRows);
      } else {
        await exportToPdf(`${filenameBase}.pdf`, title, exportColumns, exportRows);
      }
    } finally {
      setExporting(false);
    }
  }

  return (
    <Paper variant="outlined">
      <Toolbar sx={{ pl: 2, pr: 1 }}>
        <Typography variant="subtitle1" fontWeight={600} sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        <Button
          size="small"
          startIcon={exporting ? <CircularProgress size={16} /> : <DownloadIcon />}
          onClick={(e) => setExportAnchor(e.currentTarget)}
          disabled={exporting || totalElements === 0}
        >
          Export
        </Button>
        <Menu anchorEl={exportAnchor} open={Boolean(exportAnchor)} onClose={() => setExportAnchor(null)}>
          <MenuItem onClick={() => handleExport('csv')}>CSV</MenuItem>
          <MenuItem onClick={() => handleExport('xlsx')}>Excel (.xlsx)</MenuItem>
          <MenuItem onClick={() => handleExport('pdf')}>PDF</MenuItem>
        </Menu>
      </Toolbar>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell width={56}>#</TableCell>
              {columns.map((c) => (
                <TableCell key={c.key} align={c.align}>
                  {c.header}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={columns.length + 1}>
                  <Box display="flex" justifyContent="center" py={2}>
                    <CircularProgress size={24} />
                  </Box>
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              rows.map((row, index) => (
                <TableRow key={getRowId(row)} hover>
                  <TableCell>{page * pageSize + index + 1}</TableCell>
                  {columns.map((c) => (
                    <TableCell key={c.key} align={c.align}>
                      {c.render(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            {!loading && rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={columns.length + 1}>{emptyMessage}</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={totalElements}
        page={page}
        onPageChange={(_e, newPage) => onPageChange(newPage)}
        rowsPerPage={pageSize}
        onRowsPerPageChange={(e) => onPageSizeChange(Number(e.target.value))}
        rowsPerPageOptions={[10, 20, 50, 100]}
      />
    </Paper>
  );
}
