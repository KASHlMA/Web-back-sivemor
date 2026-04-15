import { useMemo } from "react";
import { useNavigate, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  EmptyState,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton,
  StatusChip
} from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

export function WebVerificationsPage() {
  const navigate = useNavigate();
  const query = useQuery({
    queryKey: ["web-verifications"],
    queryFn: () => api.get("/admin/web-verifications")
  });

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Verificaciones web"
          subtitle="Consulta las ultimas verificaciones recibidas desde movil y abre el detalle completo del formulario."
        />

        {query.isLoading ? (
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando verificaciones...</div>
        ) : query.isError ? (
          <EmptyState
            title="No fue posible cargar las verificaciones"
            description="Intenta nuevamente en unos momentos."
          />
        ) : (query.data ?? []).length === 0 ? (
          <EmptyState
            title="Aun no hay verificaciones registradas"
            description="Cuando una inspeccion movil se envie y se sincronice con el MER nuevo aparecera aqui."
          />
        ) : (
          <div className="table-shell px-3 pb-4 md:px-5">
            <table className="table-grid">
              <thead>
                <tr>
                  <th>ID verificacion</th>
                  <th>Placa</th>
                  <th>Empresa</th>
                  <th>Numero de nota</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {(query.data ?? []).map((item) => (
                  <tr key={item.verificacionId}>
                    <td>{item.verificacionId}</td>
                    <td>{item.vehiclePlate}</td>
                    <td>{item.clientCompanyName}</td>
                    <td>{item.noteNumber}</td>
                    <td>
                      <StatusChip
                        label={item.statusLabel}
                        tone={item.approved ? "success" : "danger"}
                      />
                    </td>
                    <td>{formatDateTime(item.submittedAt)}</td>
                    <td>
                      <PrimaryActionButton
                        type="button"
                        onClick={() =>
                          void navigate({
                            to: "/web-verifications/$id",
                            params: { id: String(item.verificacionId) }
                          })
                        }
                      >
                        Ver detalles
                      </PrimaryActionButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </PagePanel>
    </div>
  );
}

export function WebVerificationDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const verificationId = String(params.id ?? "");

  const query = useQuery({
    queryKey: ["web-verification-detail", verificationId],
    queryFn: () => api.get(`/admin/web-verifications/${verificationId}`),
    enabled: Boolean(verificationId)
  });

  const orderedSections = useMemo(() => {
    const sections = query.data?.sections ?? {};
    const preferredOrder = ["luces", "llantas", "direccion", "aire_frenos", "motor_emisiones", "otros", "general"];
    return preferredOrder.filter((key) => sections[key]).map((key) => [key, sections[key]]);
  }, [query.data]);

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificacion"
          subtitle={
            query.data
              ? `Verificacion ${query.data.verificacionId} para la nota ${query.data.orderNumber}.`
              : "Consulta la informacion capturada desde el formulario movil."
          }
          actions={
            <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/web-verifications" })}>
              Volver
            </SecondaryActionButton>
          }
        />

        {query.isLoading ? (
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando detalle...</div>
        ) : query.isError || !query.data ? (
          <EmptyState
            title="No fue posible cargar el detalle"
            description="La verificacion solicitada no esta disponible."
          />
        ) : (
          <div className="space-y-6 px-5 py-5">
            <section className="grid gap-4 rounded-2xl border border-[var(--border)] bg-[rgba(238,242,228,0.68)] p-5 md:grid-cols-3">
              <SummaryField label="ID verificacion" value={query.data.verificacionId} />
              <SummaryField label="Placa" value={query.data.vehiclePlate} />
              <SummaryField label="Empresa" value={query.data.clientCompanyName} />
              <SummaryField label="Numero de nota" value={query.data.orderNumber} />
              <SummaryField label="Resultado" value={query.data.overallResult ?? "-"} />
              <SummaryField label="Fecha" value={formatDateTime(query.data.submittedAt)} />
            </section>

            {orderedSections.map(([sectionName, values]) => (
              <section key={sectionName} className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
                <h2 className="text-lg font-bold capitalize text-[var(--title)]">
                  {formatSectionName(sectionName)}
                </h2>
                <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                  {Object.entries(values).map(([field, value]) => (
                    <article
                      key={field}
                      className={field === "comment" || field === "comentarios_generales" ? "md:col-span-2 xl:col-span-3" : ""}
                    >
                      <p className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                        {formatFieldName(field)}
                      </p>
                      <div className="mt-2 rounded-xl border border-[var(--border)] bg-[var(--page)] px-4 py-3 text-sm font-semibold text-[var(--title)]">
                        {formatValue(value)}
                      </div>
                    </article>
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}
      </PagePanel>
    </div>
  );
}

function SummaryField({ label, value }) {
  return (
    <div>
      <p className="text-xs font-bold uppercase tracking-[0.05em] text-[var(--shell-text)]/75">{label}</p>
      <p className="mt-1 text-sm font-semibold text-[var(--title)]">{value ?? "-"}</p>
    </div>
  );
}

function formatSectionName(value) {
  return value.replaceAll("_", " ");
}

function formatFieldName(value) {
  return value.replaceAll("_", " ");
}

function formatValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return String(value);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return new Intl.DateTimeFormat("es-MX", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
