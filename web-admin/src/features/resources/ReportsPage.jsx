import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton
} from "../../components/AdminPrimitives";
import { renderStatusValue } from "../../components/ResourceTablePage";
import { api } from "../../lib/api";

function useLookup(key, path) {
  return useQuery({
    queryKey: [key],
    queryFn: () => api.get(path)
  });
}

export function ReportsPage() {
  const [selectedInspectionId, setSelectedInspectionId] = useState("");
  const [filters, setFilters] = useState({
    companyId: "",
    regionId: "",
    orderId: "",
    technicianId: "",
    vehicleId: "",
    onlyFailures: "false"
  });

  const clients = useLookup("clients-lookup", "/admin/clients");
  const regions = useLookup("regions-lookup", "/admin/regions");
  const users = useLookup("users-lookup", "/admin/users");
  const vehicles = useLookup("vehicles-lookup", "/admin/vehicles");
  const orders = useLookup("orders-lookup", "/admin/orders");

  const queryString = useMemo(() => {
    const params = new URLSearchParams();
    if (filters.companyId) params.set("companyId", filters.companyId);
    if (filters.regionId) params.set("regionId", filters.regionId);
    if (filters.orderId) params.set("orderId", filters.orderId);
    if (filters.technicianId) params.set("technicianId", filters.technicianId);
    if (filters.vehicleId) params.set("vehicleId", filters.vehicleId);
    params.set("onlyFailures", filters.onlyFailures);
    return params.toString();
  }, [filters]);

  const reports = useQuery({
    queryKey: ["reports", queryString],
    queryFn: () => api.get(`/admin/reports?${queryString}`)
  });

  const reportDetail = useQuery({
    queryKey: ["report-detail", selectedInspectionId],
    queryFn: () => api.get(`/admin/reports/${selectedInspectionId}`),
    enabled: Boolean(selectedInspectionId)
  });

  const resetFilters = () =>
    setFilters({
      companyId: "",
      regionId: "",
      orderId: "",
      technicianId: "",
      vehicleId: "",
      onlyFailures: "false"
    });

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Reportes de fallas"
          subtitle="Filtra por empresa, región, pedido, técnico y unidad para concentrarte en las fallas detectadas."
          actions={
            <SecondaryActionButton type="button" onClick={resetFilters}>
              Limpiar
            </SecondaryActionButton>
          }
        />

        <div className="grid gap-3 p-5 md:grid-cols-2 xl:grid-cols-3">
          <FilterSelect
            label="Empresa"
            value={filters.companyId}
            onChange={(value) => setFilters((current) => ({ ...current, companyId: value }))}
            options={(clients.data ?? []).map((item) => ({ label: item.name, value: String(item.id) }))}
          />
          <FilterSelect
            label="Región"
            value={filters.regionId}
            onChange={(value) => setFilters((current) => ({ ...current, regionId: value }))}
            options={(regions.data ?? []).map((item) => ({ label: item.name, value: String(item.id) }))}
          />
          <FilterSelect
            label="Pedido"
            value={filters.orderId}
            onChange={(value) => setFilters((current) => ({ ...current, orderId: value }))}
            options={(orders.data ?? []).map((item) => ({ label: item.orderNumber, value: String(item.id) }))}
          />
          <FilterSelect
            label="Técnico"
            value={filters.technicianId}
            onChange={(value) => setFilters((current) => ({ ...current, technicianId: value }))}
            options={(users.data ?? [])
              .filter((item) => item.role === "TECHNICIAN")
              .map((item) => ({ label: item.fullName, value: String(item.id) }))}
          />
          <FilterSelect
            label="Unidad"
            value={filters.vehicleId}
            onChange={(value) => setFilters((current) => ({ ...current, vehicleId: value }))}
            options={(vehicles.data ?? []).map((item) => ({ label: item.plate, value: String(item.id) }))}
          />
          <FilterSelect
            label="Visibilidad"
            value={filters.onlyFailures}
            onChange={(value) => setFilters((current) => ({ ...current, onlyFailures: value }))}
            options={[
              { label: "Solo fallas", value: "true" },
              { label: "Todos", value: "false" }
            ]}
          />
        </div>

        <div className="table-shell px-3 pb-4 md:px-5">
          <table className="table-grid">
            <thead>
              <tr>
                <th>Pedido</th>
                <th>Empresa</th>
                <th>Región</th>
                <th>Técnico</th>
                <th>Unidad</th>
                <th>Resultado</th>
                <th>Fallas</th>
                <th>Evidencias</th>
              </tr>
            </thead>
            <tbody>
              {(reports.data ?? []).map((report) => (
                <tr
                  key={report.inspectionId}
                  onClick={() => setSelectedInspectionId(String(report.inspectionId))}
                  className="cursor-pointer"
                >
                  <td>{report.orderNumber}</td>
                  <td>{report.clientCompanyName}</td>
                  <td>{report.regionName}</td>
                  <td>{report.technicianName}</td>
                  <td>{report.vehiclePlate}</td>
                  <td>{renderStatusValue(report.overallResult ?? "PENDING")}</td>
                  <td>{renderStatusValue(report.failureCount > 0 ? "FAIL" : "PASS")}</td>
                  <td>{report.evidenceCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {selectedInspectionId ? (
          <div className="px-5 pb-5">
            <PagePanel>
              <PageTitleBar
                title="Detalle de evaluacion"
                subtitle={
                  reportDetail.data
                    ? `Fuente ${reportDetail.data.source === "MER" ? "nuevo MER" : "anterior"} para la inspección ${reportDetail.data.orderNumber}.`
                    : "Cargando detalle..."
                }
                actions={
                  <PrimaryActionButton type="button" onClick={() => setSelectedInspectionId("")}>
                    Cerrar
                  </PrimaryActionButton>
                }
              />
              {reportDetail.data ? (
                <div className="grid gap-3 p-5 md:grid-cols-2">
                  {Object.entries(reportDetail.data.sections ?? {}).map(([sectionName, values]) => (
                    <section key={sectionName} className="rounded-lg border border-[var(--border)] p-4">
                      <h4 className="text-sm font-bold uppercase tracking-[0.14em] text-[var(--title)]">{sectionName}</h4>
                      <div className="mt-3 space-y-2">
                        {Object.entries(values).map(([field, value]) => (
                          <div key={field} className="flex items-center justify-between gap-4 text-sm">
                            <span className="text-[var(--shell-text)]">{field}</span>
                            <span className="font-semibold text-[var(--title)]">{String(value ?? "-")}</span>
                          </div>
                        ))}
                      </div>
                    </section>
                  ))}
                </div>
              ) : null}
            </PagePanel>
          </div>
        ) : null}
      </PagePanel>
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  return (
    <label>
      <span className="field-label">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="field-base">
        <option value="">Todos</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}
