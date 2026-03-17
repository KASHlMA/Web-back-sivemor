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

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  component: LoginPage
});

const dashboardRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: () => (
    <ProtectedPage>
      <DashboardPage />
    </ProtectedPage>
  )
});

const usersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/users",
  component: () => (
    <ProtectedPage>
      <UsersPage />
    </ProtectedPage>
  )
});

const regionsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/regions",
  component: () => (
    <ProtectedPage>
      <RegionsPage />
    </ProtectedPage>
  )
});

const clientsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/clients",
  component: () => (
    <ProtectedPage>
      <ClientsPage />
    </ProtectedPage>
  )
});

const vehiclesRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/vehicles",
  component: () => (
    <ProtectedPage>
      <VehiclesPage />
    </ProtectedPage>
  )
});

const ordersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/orders",
  component: () => (
    <ProtectedPage>
      <OrdersPage />
    </ProtectedPage>
  )
});

const paymentsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/payments",
  component: () => (
    <ProtectedPage>
      <PaymentsPage />
    </ProtectedPage>
  )
});

const reportsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/reports",
  component: () => (
    <ProtectedPage>
      <ReportsPage />
    </ProtectedPage>
  )
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
