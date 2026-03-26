import { useQuery } from "@tanstack/react-query";
import { PageBadge, PagePanel, PageTitleBar, StatusChip } from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

export function DashboardPage() {
  const query = useQuery({
    queryKey: ["dashboard-failures"],
    queryFn: () => api.get("/admin/dashboard/failures")
  });

  const data = query.data;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Inspecciones enviadas" value={data?.totalSubmitted ?? 0} />
        <MetricCard label="Inspecciones con fallas" value={data?.totalFailed ?? 0} />
        <MetricCard label="Unidades con problemas" value={data?.unitsWithProblems ?? 0} />
      </div>

      <PagePanel>
        <PageTitleBar
          title="Resumen de fallas"
          subtitle="Vista priorizada de unidades con problemas, fallas por región y reportes recientes."
        />

        <div className="grid lg:grid-cols-[360px_minmax(0,1fr)]">
          <section className="border-b border-[var(--border)] p-5 lg:border-b-0 lg:border-r">
            <h3 className="text-[1.3rem] font-bold text-[var(--title)]">Fallas por región</h3>
            <div className="mt-4 space-y-3">
              {(data?.failuresByRegion ?? []).map((item) => (
                <div
                  key={item.label}
                  className="flex items-center justify-between rounded-lg border border-[var(--border)] bg-[rgba(238,242,228,0.8)] px-4 py-3"
                >
                  <span className="text-sm text-[var(--shell-text)]">{item.label}</span>
                  <PageBadge>{item.count}</PageBadge>
                </div>
              ))}
            </div>
          </section>

          <section className="p-5">
            <h3 className="text-[1.3rem] font-bold text-[var(--title)]">Reportes fallidos recientes</h3>
            <div className="table-shell mt-4">
              <table className="table-grid">
                <thead>
                  <tr>
                    <th>Pedido</th>
                    <th>Empresa</th>
                    <th>Región</th>
                    <th>Técnico</th>
                    <th>Unidad</th>
                    <th>Fallas</th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.recentFailures ?? []).map((report) => (
                    <tr key={report.inspectionId}>
                      <td>{report.orderNumber}</td>
                      <td>{report.clientCompanyName}</td>
                      <td>{report.regionName}</td>
                      <td>{report.technicianName}</td>
                      <td>{report.vehiclePlate}</td>
                      <td>
                        <StatusChip
                          label={String(report.failureCount)}
                          tone={report.failureCount > 0 ? "danger" : "success"}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </PagePanel>
    </div>
  );
}

function MetricCard({ label, value }) {
  return (
    <div className="app-panel px-5 py-4">
      <p className="text-sm text-[var(--shell-text)]">{label}</p>
      <p className="mt-3 text-[2.1rem] font-extrabold leading-none text-[var(--title)]">{value}</p>
    </div>
  );
}
