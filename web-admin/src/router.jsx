import { Outlet, createRootRoute, createRoute, createRouter } from "@tanstack/react-router";
import { ProtectedPage } from "./components/AppShell";
import { LoginPage } from "./features/auth/LoginPage";
import {
  CedisPage,
  ClientsPage,
  OrdersPage,
  PaymentsPage,
  PhysicalDocumentOrdersPage,
  VerificationCentersPage,
  VehiclesPage
} from "./features/resources/ResourcePages";
import { PhysicalDocumentOrderDetailPage } from "./features/resources/PhysicalDocumentOrdersPage";
import { TransactionDetailPage } from "./features/resources/TransactionsPage";
import { UsersPage } from "./features/resources/UsersPage";
import { ClientCreatePage, ClientDetailPage, ClientEditPage } from "./features/resources/ClientPages";
import { CedisCreatePage, CedisDetailPage, CedisEditPage } from "./features/resources/CedisPages";
import {
  VerificationCenterCreatePage,
  VerificationCenterDetailPage,
  VerificationCenterEditPage
} from "./features/resources/VerificationCenterPages";
import { VehicleCreatePage, VehicleDetailPage, VehicleEditPage } from "./features/resources/VehicleCreatePage";
import {
  VehicleHistoryPage
} from "./features/resources/VehicleHistoryPage";
import {
  ReportVerificationDetailPage,
  WebVerificationDetailPage,
  WebVerificationsPage
} from "./features/resources/WebVerificationsPage";

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
  component: withProtection(VehiclesPage)
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

const clientEditRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/clients/$id/editar",
  component: withProtection(ClientEditPage)
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

const vehicleDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehiculos/$id",
  component: withProtection(VehicleDetailPage)
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
  component: withProtection(ReportVerificationDetailPage)
});

const notesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/notes",
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

const cedisEditRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/cedis/$id/editar",
  component: withProtection(CedisEditPage)
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

const transactionsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/transactions",
  component: withProtection(PaymentsPage)
});

const transactionDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/transactions/$id",
  component: withProtection(TransactionDetailPage)
});

const physicalDocumentOrdersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/pedidos",
  component: withProtection(PhysicalDocumentOrdersPage)
});

const physicalDocumentOrderDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/pedidos/$id",
  component: withProtection(PhysicalDocumentOrderDetailPage)
});

const webVerificationsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/web-verifications",
  component: withProtection(WebVerificationsPage)
});

const webVerificationDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/web-verifications/$id",
  component: withProtection(WebVerificationDetailPage)
});

const routeTree = rootRoute.addChildren([
  loginRoute,
  dashboardRoute,
  usersRoute,
  clientsRoute,
  clientCreateRoute,
  clientDetailRoute,
  clientEditRoute,
  vehiclesRoute,
  vehiclesAliasRoute,
  vehicleCreateRoute,
  vehicleDetailRoute,
  vehicleEditRoute,
  vehicleHistoryRoute,
  verificationDetailRoute,
  notesRoute,
  cedisRoute,
  cedisCreateRoute,
  cedisDetailRoute,
  cedisEditRoute,
  verificationCentersRoute,
  verificationCenterCreateRoute,
  verificationCenterDetailRoute,
  verificationCenterEditRoute,
  transactionsRoute,
  transactionDetailRoute,
  physicalDocumentOrdersRoute,
  physicalDocumentOrderDetailRoute,
  webVerificationsRoute,
  webVerificationDetailRoute
]);

export const router = createRouter({ routeTree });
