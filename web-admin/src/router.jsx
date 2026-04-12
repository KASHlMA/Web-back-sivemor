import { Outlet, createRootRoute, createRoute, createRouter } from "@tanstack/react-router";
import { ProtectedPage } from "./components/AppShell";
import { LoginPage } from "./features/auth/LoginPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { PlaceholderPage } from "./features/navigation/PlaceholderPage";
import {
  ClientsPage,
  OrdersPage,
  UsersPage,
  VehiclesPage
} from "./features/resources/ResourcePages";
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
  component: withProtection(() => <PlaceholderPage title="CEDIS" />)
});

const verificationCentersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/verification-centers",
  component: withProtection(() => <PlaceholderPage title="Verificentros" />)
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
  vehiclesRoute,
  vehiclesAliasRoute,
  vehicleCreateRoute,
  vehicleEditRoute,
  vehicleHistoryRoute,
  verificationDetailRoute,
  ordersRoute,
  cedisRoute,
  verificationCentersRoute,
  notesRoute,
  webVerificationsRoute
]);

export const router = createRouter({ routeTree });
