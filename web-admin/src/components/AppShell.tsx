import MenuIcon from "@mui/icons-material/Menu";
import LogoutIcon from "@mui/icons-material/Logout";
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography
} from "@mui/material";
import { useEffect, useMemo, useState, type PropsWithChildren } from "react";
import { useNavigate, useRouterState } from "@tanstack/react-router";
import { useAuth } from "../lib/session";

const drawerWidth = 260;

const navigationItems = [
  { label: "Dashboard", to: "/" },
  { label: "Usuarios", to: "/users" },
  { label: "Regiones", to: "/regions" },
  { label: "Clientes", to: "/clients" },
  { label: "Unidades", to: "/vehicles" },
  { label: "Pedidos", to: "/orders" },
  { label: "Pagos", to: "/payments" },
  { label: "Reportes", to: "/reports" }
];

export function ProtectedPage({ children }: PropsWithChildren) {
  const navigate = useNavigate();
  const { session } = useAuth();

  useEffect(() => {
    if (!session) {
      void navigate({ to: "/login" });
    }
  }, [navigate, session]);

  if (!session) {
    return null;
  }

  return <AppShell>{children}</AppShell>;
}

function AppShell({ children }: PropsWithChildren) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const pathname = useRouterState({
    select: (state) => state.location.pathname
  });
  const navigate = useNavigate();
  const { session, logout } = useAuth();

  const title = useMemo(
    () => navigationItems.find((item) => item.to === pathname)?.label ?? "SIVEMOR",
    [pathname]
  );

  const drawer = (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <Toolbar>
        <Box>
          <Typography variant="h6" fontWeight={700}>
            SIVEMOR
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Plataforma administrativa
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ flexGrow: 1 }}>
        {navigationItems.map((item) => (
          <ListItemButton
            key={item.to}
            selected={pathname === item.to}
            onClick={() => {
              setMobileOpen(false);
              void navigate({ to: item.to });
            }}
          >
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="body2" fontWeight={600}>
          {session?.user.fullName}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
          {session?.user.role}
        </Typography>
        <ListItemButton
          onClick={async () => {
            await logout();
            void navigate({ to: "/login" });
          }}
        >
          <LogoutIcon sx={{ mr: 1 }} fontSize="small" />
          <ListItemText primary="Cerrar sesión" />
        </ListItemButton>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: "flex" }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` }
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMobileOpen((value) => !value)}
            sx={{ mr: 2, display: { sm: "none" } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div">
            {title}
          </Typography>
        </Toolbar>
      </AppBar>
      <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", sm: "none" },
            "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth }
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", sm: "block" },
            "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth }
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          p: 3,
          backgroundColor: "#f5f7fb",
          minHeight: "100vh"
        }}
      >
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
