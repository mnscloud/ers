import { useEffect, useState, type ReactNode } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
  Menu,
  MenuItem,
  Avatar,
  Divider,
  Tooltip,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import DashboardIcon from '@mui/icons-material/Dashboard';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import PeopleIcon from '@mui/icons-material/People';
import HistoryIcon from '@mui/icons-material/History';
import LockResetIcon from '@mui/icons-material/LockReset';
import { useAuth } from '../auth/AuthContext';
import { ChangePasswordDialog } from '../components/ChangePasswordDialog';

const DRAWER_WIDTH = 240;
const COLLAPSED_WIDTH = 64;
const COLLAPSE_STORAGE_KEY = 'ers.navCollapsed';

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/', icon: <DashboardIcon /> },
  { label: 'Data Ingestion', path: '/ingestion', icon: <UploadFileIcon /> },
  { label: 'Master Data', path: '/master-data', icon: <LibraryBooksIcon /> },
  { label: 'Matching', path: '/matching', icon: <CompareArrowsIcon /> },
  { label: 'Reconciliations', path: '/reconciliations', icon: <FactCheckIcon /> },
  { label: 'Exceptions', path: '/exceptions', icon: <ReportProblemIcon /> },
  { label: 'Adjustments', path: '/adjustments', icon: <ReceiptLongIcon /> },
  { label: 'Admin', path: '/admin', icon: <PeopleIcon /> },
  { label: 'Audit Log', path: '/audit', icon: <HistoryIcon /> },
];

export function AppLayout({ children }: { children: ReactNode }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem(COLLAPSE_STORAGE_KEY) === 'true');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const location = useLocation();
  const { user, logout } = useAuth();

  useEffect(() => {
    localStorage.setItem(COLLAPSE_STORAGE_KEY, String(collapsed));
  }, [collapsed]);

  const railCollapsed = collapsed && !isMobile;
  const drawerWidth = railCollapsed ? COLLAPSED_WIDTH : DRAWER_WIDTH;

  function handleMenuButtonClick() {
    if (isMobile) {
      setMobileOpen((prev) => !prev);
    } else {
      setCollapsed((prev) => !prev);
    }
  }

  const drawerContent = (
    <List>
      {NAV_ITEMS.map((item) => {
        const button = (
          <ListItemButton
            key={item.path}
            component={RouterLink}
            to={item.path}
            selected={location.pathname === item.path}
            onClick={() => isMobile && setMobileOpen(false)}
            sx={railCollapsed ? { justifyContent: 'center', px: 2.5 } : undefined}
          >
            <ListItemIcon sx={railCollapsed ? { minWidth: 0, justifyContent: 'center' } : undefined}>
              {item.icon}
            </ListItemIcon>
            {!railCollapsed && <ListItemText primary={item.label} />}
          </ListItemButton>
        );
        return railCollapsed ? (
          <Tooltip key={item.path} title={item.label} placement="right">
            {button}
          </Tooltip>
        ) : (
          button
        );
      })}
    </List>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: (t) => t.zIndex.drawer + 1,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          <IconButton color="inherit" edge="start" onClick={handleMenuButtonClick} sx={{ mr: 2 }}>
            {isMobile ? <MenuIcon /> : collapsed ? <ChevronRightIcon /> : <ChevronLeftIcon />}
          </IconButton>
          <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
            Enterprise Reconciliation System
          </Typography>
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} size="small">
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
              {user?.username?.[0]?.toUpperCase() ?? '?'}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.fullName}</MenuItem>
            <MenuItem disabled sx={{ fontSize: 12, color: 'text.secondary' }}>
              {user?.roles.join(', ')}
            </MenuItem>
            <Divider />
            <MenuItem
              onClick={() => {
                setAnchorEl(null);
                setChangePasswordOpen(true);
              }}
            >
              <LockResetIcon fontSize="small" sx={{ mr: 1 }} />
              Change Password
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => logout()}>Sign out</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <ChangePasswordDialog open={changePasswordOpen} onClose={() => setChangePasswordOpen(false)} />

      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant={isMobile ? 'temporary' : 'permanent'}
          open={isMobile ? mobileOpen : true}
          onClose={() => setMobileOpen(false)}
          slotProps={{
            root: { keepMounted: true },
            paper: {
              sx: {
                boxSizing: 'border-box',
                width: isMobile ? DRAWER_WIDTH : drawerWidth,
                overflowX: 'hidden',
              },
            },
          }}
        >
          <Toolbar />
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, md: 3 },
          width: { md: `calc(100% - ${drawerWidth}px)` },
        }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
