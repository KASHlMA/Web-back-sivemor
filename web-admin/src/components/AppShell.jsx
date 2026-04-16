import { useEffect, useMemo, useState } from "react";
import { useNavigate, useRouterState } from "@tanstack/react-router";
import { brandAssets } from "../lib/brand";
import { useAuth } from "../lib/session";
import { LogoutIcon, MenuIcon, cx } from "./AdminPrimitives";

const navigationItems = [
  { label: "Vehiculos", to: "/vehicles" },
  { label: "Verificaciones web", to: "/web-verifications" },
  { label: "Notas", to: "/notes" },
  { label: "Pedidos", to: "/pedidos" },
  { label: "Transacciones", to: "/transactions" },
  { label: "Clientes", to: "/clients" },
  { label: "CEDIS", to: "/cedis" },
  { label: "Verificentros", to: "/verification-centers" },
  { label: "Usuarios", to: "/users" }
];

export function ProtectedPage({ children }) {
  const navigate = useNavigate();
  const { session, logout, isAdmin } = useAuth();

  useEffect(() => {
    if (!session) {
      void navigate({ to: "/login" });
    }
  }, [navigate, session]);

  useEffect(() => {
    if (session && !isAdmin) {
      void logout().finally(() => {
        void navigate({ to: "/login" });
      });
    }
  }, [isAdmin, logout, navigate, session]);

  if (!session || !isAdmin) {
    return null;
  }

  return <AppShell>{children}</AppShell>;
}

function AppShell({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const pathname = useRouterState({
    select: (state) => state.location.pathname
  });
  const navigate = useNavigate();
  const { session, logout } = useAuth();
  const vehiclesSelected = pathname === "/" || pathname === "/vehicles" || pathname.startsWith("/vehiculos");
  const cedisSelected = pathname === "/cedis" || pathname.startsWith("/cedis/");
  const verificationCentersSelected = pathname === "/verification-centers" || pathname.startsWith("/verification-centers/");
  const transactionsSelected = pathname === "/transactions" || pathname.startsWith("/transactions/");
  const webVerificationsSelected = pathname === "/web-verifications" || pathname.startsWith("/web-verifications/");

  const title = useMemo(() => {
    if (pathname === "/" || pathname === "/vehicles" || pathname.startsWith("/vehiculos")) {
      return "Vehiculos";
    }

    if (pathname.startsWith("/cedis")) {
      return "CEDIS";
    }

    if (pathname.startsWith("/verification-centers")) {
      return "Verificentros";
    }

    if (pathname.startsWith("/transactions")) {
      return "Transacciones";
    }

    if (pathname.startsWith("/web-verifications")) {
      return "Verificaciones web";
    }

    return navigationItems.find((item) => item.to === pathname)?.label ?? "SIVEMOR";
  }, [pathname]);

  const navigateTo = (to) => {
    setMobileOpen(false);
    void navigate({ to });
  };

  return (
    <div className="min-h-screen bg-[var(--page)]">
      <header className="fixed inset-x-0 top-0 z-40 border-b-[18px] border-b-[var(--header-accent)] bg-[var(--header)] text-white">
        <div className="flex min-h-[120px] items-center gap-4 px-4 py-5 md:px-6">
          <button
            type="button"
            onClick={() => setMobileOpen((value) => !value)}
            className="inline-flex rounded-md p-2 transition hover:bg-white/10 lg:hidden"
            aria-label="Abrir menu"
          >
            <MenuIcon />
          </button>
          <img src={brandAssets.logoLight} alt="SIVEMOR" className="w-[74px] md:w-[92px]" />
          <div className="min-w-0 flex-1">
            <h1 className="text-[2rem] font-bold leading-none">SIVEMOR</h1>
            <p className="mt-1 text-sm text-white/95">Sistema de Verificacion de Morelos</p>
          </div>
          <p className="hidden text-base font-bold tracking-[0.02em] md:block">{title}</p>
        </div>
      </header>

      <div className="flex pt-[138px]">
        <aside
          className={cx(
            "fixed bottom-0 left-0 top-[120px] z-30 w-[246px] border-r border-r-[var(--border-strong)] bg-[var(--panel)] transition-transform lg:translate-x-0",
            mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          )}
        >
          <div className="h-full overflow-y-auto">
            <div className="min-h-[72px] border-b border-b-[var(--border-strong)] bg-[var(--shell-dark)]" />
            <nav className="flex flex-col">
              {navigationItems.map((item) => {
                const selected =
                  item.to === "/vehicles"
                    ? vehiclesSelected
                    : item.to === "/cedis"
                      ? cedisSelected
                      : item.to === "/verification-centers"
                        ? verificationCentersSelected
                        : item.to === "/transactions"
                          ? transactionsSelected
                          : item.to === "/web-verifications"
                            ? webVerificationsSelected
                            : pathname === item.to;

                return (
                  <button
                    key={item.to}
                    type="button"
                    onClick={() => navigateTo(item.to)}
                    className={cx(
                      "flex min-h-[56px] items-center gap-3 border-b border-b-[var(--border)] px-5 text-left text-[15px] font-semibold text-[var(--shell-text)] transition",
                      selected ? "bg-[#d7ddcb]" : "bg-[var(--panel)] hover:bg-[#eef4e8]"
                    )}
                  >
                    <span className={selected ? "font-bold" : ""}>{item.label}</span>
                  </button>
                );
              })}
            </nav>
            <div className="border-t border-t-[var(--border)] bg-[rgba(238,242,228,0.8)] px-5 py-4">
              <p className="text-sm font-bold text-[var(--title)]">{session?.user.fullName}</p>
              <p className="mb-4 mt-1 text-xs text-[var(--shell-text)]/80">{session?.user.role}</p>
              <button
                type="button"
                onClick={async () => {
                  await logout();
                  void navigate({ to: "/login" });
                }}
                className="flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--shell-dark-strong)] px-3 py-2.5 text-sm font-bold text-white transition hover:bg-[#0f3023]"
              >
                <LogoutIcon />
                Cerrar sesion
              </button>
            </div>
          </div>
        </aside>

        {mobileOpen ? (
          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            className="fixed inset-0 top-[120px] z-20 bg-[#173126]/30 lg:hidden"
            aria-label="Cerrar menu"
          />
        ) : null}

        <main className="min-h-screen flex-1 px-4 pb-8 pt-4 md:px-6 lg:ml-[246px] lg:px-8 lg:pt-6">
          {children}
        </main>
      </div>
    </div>
  );
}
