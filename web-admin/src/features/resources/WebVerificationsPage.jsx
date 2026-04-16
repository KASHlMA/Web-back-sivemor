import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertMessage,
  EmptyState,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton,
  StatusChip
} from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

const SECTION_ORDER = ["luces", "llantas", "direccion", "aire_frenos", "motor_emisiones", "otros", "general"];
const ENUM_OPTIONS = [
  { label: "Aprobado", value: "APROBADO" },
  { label: "Reprobado", value: "REPROBADO" },
  { label: "No aplica", value: "NO_APLICA" }
];

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
          subtitle="Consulta las ultimas verificaciones recibidas desde movil, visualizalas en lista y abre el detalle editable del formulario."
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
            description={"Cuando una inspeccion movil se envie y se sincronice aparecera aqui."}
          />
        ) : (
          <div className="table-shell px-3 pb-4 md:px-5">
            <table className="table-grid">
              <thead>
                <tr>
                  <th>ID verificacion</th>
                  <th>Placa</th>
                  <th>Empresa</th>
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
                    <td>
                      <StatusChip label={item.statusLabel} tone={item.approved ? "success" : "danger"} />
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
                        Ver y editar
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
  const queryClient = useQueryClient();
  const params = useParams({ strict: false });
  const verificationId = String(params.id ?? "");
  const [draftSections, setDraftSections] = useState({});
  const [feedbackMessage, setFeedbackMessage] = useState("");

  const query = useQuery({
    queryKey: ["web-verification-detail", verificationId],
    queryFn: () => api.get(`/admin/web-verifications/${verificationId}`),
    enabled: Boolean(verificationId)
  });

  useEffect(() => {
    if (query.data?.sections) {
      setDraftSections(cloneSections(query.data.sections));
      setFeedbackMessage("");
    }
  }, [query.data]);

  const mutation = useMutation({
    mutationFn: (payload) => api.put(`/admin/web-verifications/${verificationId}`, payload),
    onSuccess: async (response) => {
      setDraftSections(cloneSections(response.sections ?? {}));
      setFeedbackMessage("Cambios guardados correctamente.");
      await queryClient.invalidateQueries({ queryKey: ["web-verification-detail", verificationId] });
      await queryClient.invalidateQueries({ queryKey: ["web-verifications"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No fue posible guardar los cambios.");
    }
  });

  const orderedSections = useMemo(
    () => SECTION_ORDER.filter((key) => draftSections[key]).map((key) => [key, draftSections[key]]),
    [draftSections]
  );

  const handleValueChange = (sectionName, field, value) => {
    setDraftSections((current) => ({
      ...current,
      [sectionName]: {
        ...(current[sectionName] ?? {}),
        [field]: value
      }
    }));
  };

  const handleSave = () => {
    setFeedbackMessage("");
    mutation.mutate({ sections: draftSections });
  };

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificacion"
          subtitle={
            query.data
              ? `Verificacion ${query.data.verificacionId} capturada desde el formulario movil.`
              : "Consulta y edita la informacion capturada desde el formulario movil."
          }
          actions={
            <div className="flex gap-3">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/web-verifications" })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton type="button" onClick={handleSave} disabled={mutation.isPending || !query.data}>
                {mutation.isPending ? "Guardando..." : "Guardar cambios"}
              </PrimaryActionButton>
            </div>
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
            <AlertMessage message={feedbackMessage} />

            <section className="grid gap-4 rounded-2xl border border-[var(--border)] bg-[rgba(238,242,228,0.68)] p-5 md:grid-cols-3">
              <SummaryField label="ID verificacion" value={query.data.verificacionId} />
              <SummaryField label="Placa" value={query.data.vehiclePlate} />
              <SummaryField label="Empresa" value={query.data.clientCompanyName} />
              <SummaryField label="Resultado" value={query.data.overallResult ?? "-"} />
              <SummaryField label="Fecha" value={formatDateTime(query.data.submittedAt)} />
            </section>

            {orderedSections.map(([sectionName, values]) => (
              <section key={sectionName} className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
                <h2 className="text-lg font-bold capitalize text-[var(--title)]">{formatSectionName(sectionName)}</h2>
                <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                  {Object.entries(values).map(([field, value]) => (
                    <article
                      key={field}
                      className={field === "comment" || field === "comentarios_generales" ? "md:col-span-2 xl:col-span-3" : ""}
                    >
                      <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                        {formatFieldName(field)}
                      </label>
                      <EditableField
                        field={field}
                        value={value}
                        onChange={(nextValue) => handleValueChange(sectionName, field, nextValue)}
                      />
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

function EditableField({ field, value, onChange }) {
  const normalizedValue = value ?? "";

  if (field === "evidence_count") {
    return (
      <input
        value={String(normalizedValue)}
        disabled
        className="field-base mt-2 cursor-not-allowed bg-slate-100"
        readOnly
      />
    );
  }

  if (field === "comment" || field === "comentarios_generales") {
    return (
      <textarea
        value={String(normalizedValue)}
        onChange={(event) => onChange(event.target.value)}
        rows={4}
        className="field-base mt-2 min-h-28"
      />
    );
  }

  if (isEnumField(value)) {
    return (
      <select
        value={String(normalizedValue)}
        onChange={(event) => onChange(event.target.value)}
        className="field-base mt-2"
      >
        <option value="">Sin valor</option>
        {ENUM_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    );
  }

  return (
    <input
      type={typeof value === "number" ? "number" : "text"}
      step={typeof value === "number" && !Number.isInteger(value) ? "0.01" : "1"}
      value={String(normalizedValue)}
      onChange={(event) => onChange(event.target.value)}
      className="field-base mt-2"
    />
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

function cloneSections(sections) {
  return Object.fromEntries(
    Object.entries(sections ?? {}).map(([sectionName, values]) => [
      sectionName,
      Object.fromEntries(Object.entries(values ?? {}).map(([field, value]) => [field, value ?? ""]))
    ])
  );
}

function isEnumField(value) {
  return ["APROBADO", "REPROBADO", "NO_APLICA"].includes(String(value ?? "").toUpperCase());
}

function formatSectionName(value) {
  return value.replaceAll("_", " ");
}

function formatFieldName(value) {
  return value.replaceAll("_", " ");
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
