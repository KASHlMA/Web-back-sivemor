import { Outlet, createRootRoute, createRoute, createRouter } from "@tanstack/react-router";
import { ProtectedPage } from "./components/AppShell";
import { LoginPage } from "./features/auth/LoginPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import {
  ClientsPage,
  OrdersPage,
  PaymentsPage,
  RegionsPage,
  UsersPage,
  VehiclesPage
} from "./features/resources/ResourcePages";
import { ReportsPage } from "./features/resources/ReportsPage";

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

const regionsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/regions",
  component: withProtection(RegionsPage)
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

const ordersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/orders",
  component: withProtection(OrdersPage)
});

const paymentsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/payments",
  component: withProtection(PaymentsPage)
});

const reportsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/reports",
  component: withProtection(ReportsPage)
});

const routeTree = rootRoute.addChildren([
  loginRoute,
  dashboardRoute,
  usersRoute,
  regionsRoute,
  clientsRoute,
  vehiclesRoute,
  ordersRoute,
  paymentsRoute,
  reportsRoute
]);

export const router = createRouter({ routeTree });
