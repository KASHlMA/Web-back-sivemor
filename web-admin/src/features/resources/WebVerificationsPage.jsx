import { useEffect, useState } from "react";
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
                  <th>Placa</th>
                  <th>Serie</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {(query.data ?? []).map((item) => (
                  <tr key={item.verificacionId}>
                    <td>{item.vehiclePlate}</td>
                    <td>{item.vehicleVin ?? "-"}</td>
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
  const [draftFormSections, setDraftFormSections] = useState([]);
  const [overallComment, setOverallComment] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");

  const query = useQuery({
    queryKey: ["web-verification-detail", verificationId],
    queryFn: () => api.get(`/admin/web-verifications/${verificationId}`),
    enabled: Boolean(verificationId)
  });

  useEffect(() => {
    if (query.data?.formSections) {
      setDraftFormSections(cloneFormSections(query.data.formSections));
      setOverallComment(query.data.overallComment ?? "");
      setFeedbackMessage("");
    }
  }, [query.data]);

  const mutation = useMutation({
    mutationFn: (payload) => api.put(`/admin/web-verifications/${verificationId}`, payload),
    onSuccess: async (response) => {
      setDraftFormSections(cloneFormSections(response.formSections ?? []));
      setOverallComment(response.overallComment ?? "");
      setFeedbackMessage("Cambios guardados correctamente.");
      await queryClient.invalidateQueries({ queryKey: ["web-verification-detail", verificationId] });
      await queryClient.invalidateQueries({ queryKey: ["web-verifications"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No fue posible guardar los cambios.");
    }
  });

  const handleSectionNoteChange = (sectionId, value) => {
    setDraftFormSections((current) =>
      current.map((section) => (section.sectionId === sectionId ? { ...section, note: value } : section))
    );
  };

  const handleQuestionChange = (sectionId, questionId, field, value) => {
    setDraftFormSections((current) =>
      current.map((section) =>
        section.sectionId === sectionId
          ? {
              ...section,
              questions: section.questions.map((question) =>
                question.questionId === questionId ? { ...question, [field]: value } : question
              )
            }
          : section
      )
    );
  };

  const handleSave = () => {
    setFeedbackMessage("");
    mutation.mutate({
      formSections: draftFormSections,
      sections: {
        general: {
          comentarios_generales: overallComment
        }
      }
    });
  };

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificacion"
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

            {draftFormSections.map((section) => (
              <section key={section.sectionId} className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
                <h2 className="text-lg font-bold text-[var(--title)]">{section.title}</h2>

                <div className="mt-5 space-y-4">
                  {(section.questions ?? []).map((question) => (
                    <article key={question.questionId} className="rounded-2xl border border-[var(--border)] bg-[var(--panel)] p-4">
                      <p className="text-sm font-semibold text-[var(--title)]">{question.prompt}</p>

                      <div className="mt-4">
                        <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                          Resultado
                        </label>
                        <select
                          value={String(question.answer ?? "")}
                          onChange={(event) => handleQuestionChange(section.sectionId, question.questionId, "answer", event.target.value)}
                          className="field-base mt-2"
                        >
                          <option value="">Sin valor</option>
                          {ENUM_OPTIONS.map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="mt-4">
                        <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                          Comentario
                        </label>
                        <textarea
                          value={question.comment ?? ""}
                          onChange={(event) =>
                            handleQuestionChange(section.sectionId, question.questionId, "comment", event.target.value)
                          }
                          rows={3}
                          className="field-base mt-2 min-h-24"
                        />
                      </div>
                    </article>
                  ))}
                </div>

                <div className="mt-5">
                  <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                    Comentario de seccion
                  </label>
                  <textarea
                    value={section.note ?? ""}
                    onChange={(event) => handleSectionNoteChange(section.sectionId, event.target.value)}
                    rows={3}
                    className="field-base mt-2 min-h-24"
                  />
                </div>
              </section>
            ))}

            <section className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold text-[var(--title)]">Evidencias</h2>
                <StatusChip
                  label={`${query.data.evidences?.length ?? 0} archivo${(query.data.evidences?.length ?? 0) === 1 ? "" : "s"}`}
                  tone="neutral"
                />
              </div>

              {(query.data.evidences?.length ?? 0) > 0 ? (
                <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {query.data.evidences.map((evidence) => (
                    <article key={evidence.id} className="overflow-hidden rounded-2xl border border-[var(--border)] bg-[var(--panel)]">
                      {evidence.mimeType?.startsWith("image/") ? (
                        <a href={evidence.previewUrl} target="_blank" rel="noreferrer" className="block">
                          <img
                            src={evidence.previewUrl}
                            alt={evidence.filename}
                            className="h-52 w-full bg-slate-100 object-cover"
                          />
                        </a>
                      ) : (
                        <a
                          href={evidence.previewUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="flex h-52 items-center justify-center bg-slate-100 px-4 text-center text-sm font-semibold text-[var(--title)]"
                        >
                          Abrir archivo
                        </a>
                      )}

                      <div className="space-y-2 p-4">
                        <p className="truncate text-sm font-semibold text-[var(--title)]">{evidence.filename}</p>
                        <p className="text-xs uppercase tracking-[0.05em] text-[var(--shell-text)]/70">
                          {evidence.sectionName ? formatSectionName(evidence.sectionName) : "Sin seccion"}
                        </p>
                        <p className="text-sm text-[var(--shell-text)]/80">{formatDateTime(evidence.capturedAt)}</p>
                        <p className="text-sm text-[var(--shell-text)]/80">{evidence.comment || "Sin comentario"}</p>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="mt-4 text-sm text-[var(--shell-text)]/80">Sin evidencias adjuntas.</p>
              )}
            </section>

            <section className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
              <h2 className="text-lg font-bold text-[var(--title)]">Comentarios</h2>

              <div className="mt-4">
                <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                  General
                </label>
                <textarea
                  value={overallComment}
                  onChange={(event) => setOverallComment(event.target.value)}
                  rows={4}
                  className="field-base mt-2 min-h-28"
                />
              </div>
            </section>
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

function cloneFormSections(sections) {
  return (sections ?? []).map((section) => ({
    ...section,
    note: section.note ?? "",
    questions: (section.questions ?? []).map((question) => ({
      ...question,
      answer: question.answer ?? "",
      comment: question.comment ?? ""
    }))
  }));
}

function formatSectionName(value) {
  return value.replaceAll("_", " ");
}

function formatAnswerValue(value) {
  if (!value) {
    return "Sin valor";
  }

  const option = ENUM_OPTIONS.find((item) => item.value === value);
  return option?.label ?? String(value).replaceAll("_", " ");
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
