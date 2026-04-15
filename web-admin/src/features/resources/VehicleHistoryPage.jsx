import { useMemo } from "react";
import { useNavigate, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  EmptyState,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton
} from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

export function VehicleHistoryPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const vehicleId = String(params.id ?? "");

  const reportsQuery = useQuery({
    queryKey: ["vehicle-history", vehicleId],
    queryFn: () => api.get(`/admin/reports?vehicleId=${vehicleId}&onlyFailures=false`),
    enabled: Boolean(vehicleId)
  });

  const vehiclesQuery = useQuery({
    queryKey: ["vehicles-lookup"],
    queryFn: () => api.get("/admin/vehicles")
  });

  const clientsQuery = useQuery({
    queryKey: ["clients-lookup"],
    queryFn: () => api.get("/admin/clients")
  });

  const vehicleRecord = useMemo(
    () => (vehiclesQuery.data ?? []).find((item) => String(item.id) === vehicleId) ?? null,
    [vehicleId, vehiclesQuery.data]
  );

  const cards = useMemo(() => {
    return (reportsQuery.data ?? []).map((report) => {
      const client = (clientsQuery.data ?? []).find(
        (item) => item.name === report.clientCompanyName
      );

      return {
        inspectionId: report.inspectionId,
        clientName: report.clientCompanyName,
        cedisName: client?.name ? client.name : "-",
        vehicleNumber: report.orderNumber,
        regionName: report.regionName,
        vin: vehicleRecord?.vin ?? "-",
        plate: report.vehiclePlate ?? vehicleRecord?.plate ?? "-",
        ingresoDate: formatDate(report.submittedAt),
        finishedDate: formatDate(report.submittedAt),
        approved: report.overallResult === "PASS",
        statusLabel: report.overallResult === "PASS" ? "Aprobado" : "Reprobado",
        description:
          report.failureCount > 0
            ? `Se detectaron ${report.failureCount} fallas en la verificacion`
            : "Verificacion aprobada sin fallas registradas"
      };
    });
  }, [clientsQuery.data, reportsQuery.data, vehicleRecord]);

  if (reportsQuery.isLoading || vehiclesQuery.isLoading || clientsQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Historial"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/vehiculos" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando historial...</div>
        </PagePanel>
      </div>
    );
  }

  if (reportsQuery.isError) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Historial"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/vehiculos" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="Error al cargar el historial de verificaciones"
            description="Intenta nuevamente en unos momentos."
          />
        </PagePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Historial"
          actions={
            <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/vehiculos" })}>
              Volver
            </SecondaryActionButton>
          }
        />

        <div className="space-y-4 px-5 py-5">
          {cards.length === 0 ? (
            <EmptyState
              title="No hay verificaciones registradas para este vehiculo"
              description="Cuando existan verificaciones asociadas a esta unidad apareceran aqui."
            />
          ) : (
            cards.map((card) => (
              <article
                key={card.inspectionId}
                className={`rounded-2xl border border-[var(--border)] p-5 shadow-[var(--shadow)] ${
                  card.approved ? "bg-[var(--success-bg)]" : "bg-[#fff3f0]"
                }`}
              >
                <div className="grid gap-3 md:grid-cols-2">
                  <InfoRow label="Cliente" value={card.clientName} />
                  <InfoRow label="CEDIS" value={card.cedisName} />
                  <InfoRow label="Número de vehículo" value={card.vehicleNumber} />
                  <InfoRow label="Región" value={card.regionName} />
                  <InfoRow label="Número de serie" value={card.vin} />
                  <InfoRow label="Placas" value={card.plate} />
                  <InfoRow label="Fecha de ingreso" value={card.ingresoDate} />
                  <InfoRow label="Fecha de finalización" value={card.finishedDate} />
                  <InfoRow label="Estado" value={card.statusLabel} />
                  <InfoRow label="Descripción" value={card.description} fullWidth />
                </div>

                <div className="mt-4 flex justify-end">
                  <PrimaryActionButton
                    type="button"
                    onClick={() =>
                      void navigate({
                        to: "/verificaciones/$id",
                        params: { id: String(card.inspectionId) }
                      })
                    }
                  >
                    Ver informacion
                  </PrimaryActionButton>
                </div>
              </article>
            ))
          )}
        </div>
      </PagePanel>
    </div>
  );
}

export function VerificationDetailPlaceholderPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificacion"
          subtitle={`Verificacion ${params.id}`}
          actions={
            <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/vehiculos" })}>
              Volver
            </SecondaryActionButton>
          }
        />

        <div className="px-5 py-5">
          <EmptyState
            title="El detalle de verificacion aun no esta disponible"
            description="Esta pantalla queda lista como punto de navegación compatible dentro de web-admin."
          />
        </div>
      </PagePanel>
    </div>
  );
}

function InfoRow({ label, value, fullWidth = false }) {
  return (
    <div className={fullWidth ? "md:col-span-2" : ""}>
      <p className="text-xs font-bold uppercase tracking-[0.04em] text-[var(--shell-text)]/75">{label}</p>
      <p className="mt-1 text-sm font-semibold text-[var(--title)]">{value}</p>
    </div>
  );
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("es-MX", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  }).format(date).replaceAll("/", "-");
}
