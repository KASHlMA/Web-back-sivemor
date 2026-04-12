import { Outlet, createRootRoute, createRoute, createRouter } from "@tanstack/react-router";
import { ProtectedPage } from "./components/AppShell";
import { LoginPage } from "./features/auth/LoginPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { PlaceholderPage } from "./features/navigation/PlaceholderPage";
import {
  CedisPage,
  ClientsPage,
  OrdersPage,
  VerificationCentersPage,
  UsersPage,
  VehiclesPage
} from "./features/resources/ResourcePages";
import { ClientCreatePage, ClientDetailPage } from "./features/resources/ClientPages";
import { CedisCreatePage, CedisDetailPage } from "./features/resources/CedisPages";
import {
  VerificationCenterCreatePage,
  VerificationCenterDetailPage,
  VerificationCenterEditPage
} from "./features/resources/VerificationCenterPages";
import { VehicleCreatePage, VehicleEditPage } from "./features/resources/VehicleCreatePage";
import {
  VehicleHistoryPage,
  VerificationDetailPlaceholderPage
} from "./features/resources/VehicleHistoryPage";

const rootRoute = createRootRoute({
  component: () => <Outlet />
});

const withProtection = (Component) => () => (
  <ProtectedPage>
    <Component />
  </ProtectedPage>
);

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  component: LoginPage
});

const dashboardRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: withProtection(DashboardPage)
});

const usersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/users",
  component: withProtection(UsersPage)
});

const clientsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/clients",
  component: withProtection(ClientsPage)
});

const clientCreateRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/clients/nuevo",
  component: withProtection(ClientCreatePage)
});

const clientDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/clients/$id",
  component: withProtection(ClientDetailPage)
});

const vehiclesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehicles",
  component: withProtection(VehiclesPage)
});

const vehiclesAliasRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehiculos",
  component: withProtection(VehiclesPage)
});

const vehicleCreateRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehiculos/nuevo",
  component: withProtection(VehicleCreatePage)
});

const vehicleEditRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehiculos/$id/editar",
  component: withProtection(VehicleEditPage)
});

const vehicleHistoryRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehiculos/$id/historial",
  component: withProtection(VehicleHistoryPage)
});

const verificationDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verificaciones/$id",
  component: withProtection(VerificationDetailPlaceholderPage)
});

const ordersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/orders",
  component: withProtection(OrdersPage)
});

const cedisRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/cedis",
  component: withProtection(CedisPage)
});

const cedisCreateRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/cedis/nuevo",
  component: withProtection(CedisCreatePage)
});

const cedisDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/cedis/$id",
  component: withProtection(CedisDetailPage)
});

const verificationCentersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verification-centers",
  component: withProtection(VerificationCentersPage)
});

const verificationCenterCreateRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verification-centers/nuevo",
  component: withProtection(VerificationCenterCreatePage)
});

const verificationCenterDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verification-centers/$id",
  component: withProtection(VerificationCenterDetailPage)
});

const verificationCenterEditRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verification-centers/$id/editar",
  component: withProtection(VerificationCenterEditPage)
});

const notesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/notes",
  component: withProtection(() => <PlaceholderPage title="Notas" />)
});

const webVerificationsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/web-verifications",
  component: withProtection(() => <PlaceholderPage title="Verificaciones web" />)
});

const routeTree = rootRoute.addChildren([
  loginRoute,
  dashboardRoute,
  usersRoute,
  clientsRoute,
  clientCreateRoute,
  clientDetailRoute,
  vehiclesRoute,
  vehiclesAliasRoute,
  vehicleCreateRoute,
  vehicleEditRoute,
  vehicleHistoryRoute,
  verificationDetailRoute,
  ordersRoute,
  cedisRoute,
  cedisCreateRoute,
  cedisDetailRoute,
  verificationCentersRoute,
  verificationCenterCreateRoute,
  verificationCenterDetailRoute,
  verificationCenterEditRoute,
  notesRoute,
  webVerificationsRoute
]);

export const router = createRouter({ routeTree });
