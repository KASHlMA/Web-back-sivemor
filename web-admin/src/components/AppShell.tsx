import ChevronRightRoundedIcon from "@mui/icons-material/ChevronRightRounded";
import LogoutIcon from "@mui/icons-material/Logout";
import MenuIcon from "@mui/icons-material/Menu";
import {
  alpha,
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery
} from "@mui/material";
import { useEffect, useMemo, useState, type PropsWithChildren } from "react";
import { useNavigate, useRouterState } from "@tanstack/react-router";
import { brandAssets, brandTokens } from "../lib/brand";
import { useAuth } from "../lib/session";

const drawerWidth = 246;

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
  const isLarge = useMediaQuery("(min-width:900px)");

  const title = useMemo(
    () => navigationItems.find((item) => item.to === pathname)?.label ?? "SIVEMOR",
    [pathname]
  );

  const drawer = (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        backgroundColor: brandTokens.colors.panel,
        borderRight: `1px solid ${brandTokens.colors.borderStrong}`
      }}
    >
      <Box
        sx={{
          minHeight: 72,
          backgroundColor: brandTokens.colors.shellDark,
          borderBottom: `1px solid ${brandTokens.colors.borderStrong}`
        }}
      />
      <List sx={{ flexGrow: 1, py: 0 }}>
        {navigationItems.map((item, index) => {
          const selected = pathname === item.to;
          return (
            <ListItemButton
              key={item.to}
              selected={selected}
              onClick={() => {
                setMobileOpen(false);
                void navigate({ to: item.to });
              }}
              sx={{
                minHeight: 56,
                px: 2.5,
                borderBottom: `1px solid ${brandTokens.colors.border}`,
                bgcolor: selected ? "#d7ddcb" : brandTokens.colors.panel,
                color: brandTokens.colors.shellText,
                "&.Mui-selected": {
                  bgcolor: "#d7ddcb"
                },
                "&.Mui-selected:hover": {
                  bgcolor: "#d7ddcb"
                }
              }}
            >
              <Box sx={{ width: 20, display: "flex", justifyContent: "center", opacity: selected || index % 2 === 0 ? 1 : 0.25 }}>
                <ChevronRightRoundedIcon sx={{ fontSize: 18 }} />
              </Box>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{
                  fontSize: 15,
                  fontWeight: selected ? 700 : 600
                }}
              />
            </ListItemButton>
          );
        })}
      </List>
      <Box
        sx={{
          px: 2.5,
          py: 2,
          borderTop: `1px solid ${brandTokens.colors.border}`,
          bgcolor: alpha(brandTokens.colors.panelAlt, 0.8)
        }}
      >
        <Typography variant="body2" fontWeight={700} sx={{ color: brandTokens.colors.title }}>
          {session?.user.fullName}
        </Typography>
        <Typography variant="caption" display="block" sx={{ mb: 1.5, color: alpha(brandTokens.colors.shellText, 0.8) }}>
          {session?.user.role}
        </Typography>
        <ListItemButton
          onClick={async () => {
            await logout();
            void navigate({ to: "/login" });
          }}
          sx={{
            borderRadius: 1,
            px: 1.25,
            py: 0.75,
            bgcolor: brandTokens.colors.shellDarkStrong,
            color: "#fff",
            "&:hover": {
              bgcolor: "#0f3023"
            }
          }}
        >
          <LogoutIcon sx={{ mr: 1 }} fontSize="small" />
          <ListItemText
            primary="Cerrar sesión"
            primaryTypographyProps={{ fontWeight: 700, fontSize: 14 }}
          />
        </ListItemButton>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ minHeight: "100vh", backgroundColor: brandTokens.colors.page, display: "flex" }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          backgroundColor: brandTokens.colors.header,
          color: "#fff",
          borderBottom: `18px solid ${brandTokens.colors.headerAccent}`
        }}
      >
        <Toolbar sx={{ minHeight: "120px !important", px: { xs: 2, md: 3 } }}>
          {!isLarge ? (
            <IconButton
              color="inherit"
              edge="start"
              onClick={() => setMobileOpen((value) => !value)}
              sx={{ mr: 1.5 }}
            >
              <MenuIcon />
            </IconButton>
          ) : null}
          <Box
            component="img"
            src={brandAssets.logoLight}
            alt="SIVEMOR"
            sx={{ width: { xs: 74, md: 92 }, height: "auto", mr: 2 }}
          />
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h4" sx={{ color: "#fff", lineHeight: 1.05 }}>
              SIVEMOR
            </Typography>
            <Typography variant="body2" sx={{ color: alpha("#ffffff", 0.96) }}>
              Sistema de Verificación de Morelos
            </Typography>
          </Box>
          <Typography
            variant="body1"
            sx={{
              display: { xs: "none", md: "block" },
              fontWeight: 700,
              letterSpacing: 0.2
            }}
          >
            {title}
          </Typography>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { lg: drawerWidth }, flexShrink: { lg: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", lg: "none" },
            "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth }
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", lg: "block" },
            "& .MuiDrawer-paper": {
              boxSizing: "border-box",
              width: drawerWidth,
              top: 120,
              height: "calc(100% - 120px)"
            }
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
          minHeight: "100vh",
          px: { xs: 2, md: 3, lg: 4 },
          pt: { xs: "150px", md: "164px" },
          pb: 4
        }}
      >
        {children}
      </Box>
    </Box>
  );
}
