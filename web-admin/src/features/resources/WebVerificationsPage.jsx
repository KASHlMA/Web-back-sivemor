import { useEffect, useState } from "react";
import { useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertMessage,
  ConfirmDialog,
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

const FORM_SECTION_CONFIG = [
  {
    key: "luces",
    title: "Luces",
    questions: [
      { code: "luces_galibo", prompt: "Luces galibo" },
      { code: "luces_altas", prompt: "Luces altas" },
      { code: "luces_bajas", prompt: "Luces bajas" },
      { code: "luces_demarcadoras_delanteras", prompt: "Luces demarcadoras delanteras" },
      { code: "luces_demarcadoras_traseras", prompt: "Luces demarcadoras traseras" },
      { code: "luces_indicadoras", prompt: "Luces indicadoras" },
      { code: "faro_izquierdo", prompt: "Faro izquierdo" },
      { code: "faro_derecho", prompt: "Faro derecho" },
      { code: "luces_direccionales_delanteras", prompt: "Luces direccionales delanteras" },
      { code: "luces_direccionales_traseras", prompt: "Luces direccionales traseras" }
    ]
  },
  {
    key: "llantas",
    title: "Llantas",
    questions: [
      { code: "llantas_rines_delanteros", prompt: "Llantas rines delanteros" },
      { code: "llantas_rines_traseros", prompt: "Llantas rines traseros" },
      { code: "llantas_masas_delanteras", prompt: "Llantas masas delanteras" },
      { code: "llantas_masas_traseras", prompt: "Llantas masas traseras" },
      { code: "llantas_presion_delantera_izquierda", prompt: "Llantas presion delantera izquierda", type: "number" },
      { code: "llantas_presion_delantera_derecha", prompt: "Llantas presion delantera derecha", type: "number" },
      { code: "llantas_presion_trasera_izquierda_1", prompt: "Llantas presion trasera izquierda 1", type: "number" },
      { code: "llantas_presion_trasera_izquierda_2", prompt: "Llantas presion trasera izquierda 2", type: "number" },
      { code: "llantas_presion_trasera_derecha_1", prompt: "Llantas presion trasera derecha 1", type: "number" },
      { code: "llantas_presion_trasera_derecha_2", prompt: "Llantas presion trasera derecha 2", type: "number" },
      { code: "llantas_profundidad_delantera_izquierda", prompt: "Llantas profundidad delantera izquierda", type: "number" },
      { code: "llantas_profundidad_delantera_derecha", prompt: "Llantas profundidad delantera derecha", type: "number" },
      { code: "llantas_profundidad_trasera_izquierda_1", prompt: "Llantas profundidad trasera izquierda 1", type: "number" },
      { code: "llantas_profundidad_trasera_izquierda_2", prompt: "Llantas profundidad trasera izquierda 2", type: "number" },
      { code: "llantas_profundidad_trasera_derecha_1", prompt: "Llantas profundidad trasera derecha 1", type: "number" },
      { code: "llantas_profundidad_trasera_derecha_2", prompt: "Llantas profundidad trasera derecha 2", type: "number" },
      { code: "llantas_tuercas_delantera_izquierda", prompt: "Llantas tuercas delantera izquierda" },
      { code: "llantas_tuercas_delantera_izquierda_faltantes", prompt: "Tuercas faltantes delantera izquierda", type: "number" },
      { code: "llantas_tuercas_delantera_izquierda_rotas", prompt: "Tuercas rotas delantera izquierda", type: "number" },
      { code: "llantas_tuercas_delantera_derecha", prompt: "Llantas tuercas delantera derecha" },
      { code: "llantas_tuercas_delantera_derecha_faltantes", prompt: "Tuercas faltantes delantera derecha", type: "number" },
      { code: "llantas_tuercas_delantera_derecha_rotas", prompt: "Tuercas rotas delantera derecha", type: "number" },
      { code: "llantas_tuercas_trasera_izquierda", prompt: "Llantas tuercas trasera izquierda" },
      { code: "llantas_tuercas_trasera_izquierda_faltantes", prompt: "Tuercas faltantes trasera izquierda", type: "number" },
      { code: "llantas_tuercas_trasera_izquierda_rotas", prompt: "Tuercas rotas trasera izquierda", type: "number" },
      { code: "llantas_tuercas_trasera_derecha", prompt: "Llantas tuercas trasera derecha" },
      { code: "llantas_tuercas_trasera_derecha_faltantes", prompt: "Tuercas faltantes trasera derecha", type: "number" },
      { code: "llantas_tuercas_trasera_derecha_rotas", prompt: "Tuercas rotas trasera derecha", type: "number" }
    ]
  },
  {
    key: "direccion",
    title: "Direccion, estructura y accesos",
    questions: [
      { code: "direccion_brazo_pitman", prompt: "Brazo pitman" },
      { code: "direccion_manijas_puertas", prompt: "Manijas de puertas" },
      { code: "direccion_chavetas", prompt: "Chavetas" },
      { code: "direccion_chavetas_faltantes", prompt: "En caso de que hagan falta chavetas", type: "number" }
    ]
  },
  {
    key: "aire_frenos",
    title: "Sistema de aire / frenos",
    questions: [
      { code: "aire_frenos_compresor", prompt: "Compresor" },
      { code: "aire_frenos_tanques_aire", prompt: "Tanques de aire" },
      { code: "aire_frenos_tiempo_carga_psi", prompt: "Tiempo de carga psi", type: "number" },
      { code: "aire_frenos_tiempo_carga_tiempo", prompt: "Tiempo de carga tiempo", type: "number" }
    ]
  },
  {
    key: "motor_emisiones",
    title: "Motor y emisiones",
    questions: [
      { code: "motor_emisiones_humo", prompt: "Humo" },
      { code: "motor_emisiones_gobernado", prompt: "Gobernado" }
    ]
  },
  {
    key: "otros",
    title: "Otros",
    questions: [
      { code: "otros_caja_direccion", prompt: "Caja direccion" },
      { code: "otros_deposito_aceite", prompt: "Deposito aceite" },
      { code: "otros_parabrisas", prompt: "Parabrisas" },
      { code: "otros_limpiaparabrisas", prompt: "Limpiaparabrisas" },
      { code: "otros_juego", prompt: "Huelgo" },
      { code: "otros_escape", prompt: "Escape" }
    ]
  }
];

export function WebVerificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [pendingDelete, setPendingDelete] = useState(null);
  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [downloadingId, setDownloadingId] = useState(null);
  const query = useQuery({
    queryKey: ["web-verifications"],
    queryFn: () => api.get("/admin/web-verifications")
  });
  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/admin/web-verifications/${id}`),
    onSuccess: async () => {
      setPendingDelete(null);
      setFeedbackMessage("Verificacion eliminada correctamente.");
      await queryClient.invalidateQueries({ queryKey: ["web-verifications"] });
      await queryClient.invalidateQueries({ queryKey: ["vehicle-history"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No fue posible eliminar la verificacion.");
    }
  });

  const handleDownloadReport = async (verification) => {
    try {
      setFeedbackMessage("");
      setDownloadingId(verification.verificacionId);
      const response = await api.download(`/admin/web-verifications/${verification.verificacionId}/report`);
      const blobUrl = window.URL.createObjectURL(response.data);
      const link = document.createElement("a");
      const filename = getFilenameFromDisposition(response.headers.get("content-disposition")) ??
        `reporte-verificacion-${verification.vehiclePlate ?? verification.verificacionId}.pdf`;
      link.href = blobUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(blobUrl);
    } catch (error) {
      setFeedbackMessage(error instanceof Error ? error.message : "No fue posible generar el reporte.");
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Verificaciones web"
          subtitle="Consulta las ultimas verificaciones recibidas desde movil, visualizalas en lista y abre el detalle editable del formulario."
        />

        <div className="px-5 pt-1">
          <AlertMessage message={feedbackMessage} />
        </div>

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
                      <div className="flex flex-wrap gap-2">
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
                        <SecondaryActionButton
                          type="button"
                          onClick={() => void handleDownloadReport(item)}
                          disabled={downloadingId === item.verificacionId}
                        >
                          {downloadingId === item.verificacionId ? "Generando..." : "Generar reporte"}
                        </SecondaryActionButton>
                        <SecondaryActionButton
                          type="button"
                          onClick={() => {
                            setFeedbackMessage("");
                            setPendingDelete(item);
                          }}
                        >
                          Eliminar
                        </SecondaryActionButton>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </PagePanel>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Eliminar verificacion web"
        description="Deseas eliminar esta verificacion web? Esta accion la ocultara de los listados y del historial administrativo."
        confirmLabel={deleteMutation.isPending ? "Eliminando..." : "Eliminar"}
        onCancel={() => {
          if (!deleteMutation.isPending) {
            setPendingDelete(null);
          }
        }}
        onConfirm={() => {
          if (pendingDelete && !deleteMutation.isPending) {
            void deleteMutation.mutateAsync(pendingDelete.verificacionId);
          }
        }}
        danger
      />
    </div>
  );
}

export function WebVerificationDetailPage() {
  const params = useParams({ strict: false });
  return (
    <EvaluationDetailPage
      detailKey="web-verification-detail"
      detailId={String(params.id ?? "")}
      loadDetail={(id) => api.get(`/admin/web-verifications/${id}`)}
      backTo="/web-verifications"
    />
  );
}

export function ReportVerificationDetailPage() {
  const params = useParams({ strict: false });
  return (
    <EvaluationDetailPage
      detailKey="report-detail"
      detailId={String(params.id ?? "")}
      loadDetail={(id) => api.get(`/admin/reports/${id}`)}
      backTo="/vehiculos"
    />
  );
}

function EvaluationDetailPage({ detailKey, detailId, loadDetail, backTo }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [draftFormSections, setDraftFormSections] = useState([]);
  const [overallComment, setOverallComment] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");

  const query = useQuery({
    queryKey: [detailKey, detailId],
    queryFn: () => loadDetail(detailId),
    enabled: Boolean(detailId)
  });

  useEffect(() => {
    if (query.data) {
      setDraftFormSections(buildDraftSections(query.data));
      setOverallComment(query.data.overallComment ?? "");
      setFeedbackMessage("");
    }
  }, [query.data]);

  const mutation = useMutation({
    mutationFn: async (payload) => {
      if (!query.data?.verificacionId) {
        throw new Error("Esta verificacion no tiene una evaluacion editable disponible.");
      }

      return api.put(`/admin/web-verifications/${query.data.verificacionId}`, payload);
    },
    onSuccess: async (response) => {
      setDraftFormSections(buildDraftSections(response));
      setOverallComment(response.overallComment ?? "");
      setFeedbackMessage("Cambios guardados correctamente.");
      await queryClient.invalidateQueries({ queryKey: [detailKey, detailId] });
      await queryClient.invalidateQueries({ queryKey: ["web-verifications"] });
      await queryClient.invalidateQueries({ queryKey: ["vehicle-history"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No fue posible guardar los cambios.");
    }
  });

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
      formSections: [],
      sections: {
        ...toSectionsPayload(draftFormSections),
        general: {
          comentarios_generales: overallComment
        }
      }
    });
  };

  const canEdit = Boolean(query.data?.verificacionId);

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificacion"
          actions={
            <div className="flex gap-3">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: backTo })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton type="button" onClick={handleSave} disabled={mutation.isPending || !query.data || !canEdit}>
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
              <SummaryField label="ID verificacion" value={query.data.verificacionId ?? "-"} />
              <SummaryField label="ID inspeccion" value={query.data.inspectionId ?? "-"} />
              <SummaryField label="Placa" value={query.data.vehiclePlate} />
              <SummaryField label="Empresa" value={query.data.clientCompanyName} />
              <SummaryField label="Resultado" value={query.data.overallResult ?? "-"} />
              <SummaryField label="Fecha" value={formatDateTime(query.data.submittedAt)} />
            </section>

            {draftFormSections.map((section, sectionIndex) => (
              <section key={section.sectionId} className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
                <div className="flex flex-col gap-2 border-b border-[var(--border)] pb-4 md:flex-row md:items-center md:justify-between">
                  <h2 className="text-lg font-bold text-[var(--title)]">
                    {sectionIndex + 1}. {section.title}
                  </h2>
                  <p className="text-sm font-medium text-[var(--shell-text)]/80">
                    {section.questions?.length ?? 0} punto{(section.questions?.length ?? 0) === 1 ? "" : "s"}
                  </p>
                </div>

                <div className="mt-5 space-y-4">
                  {(section.questions ?? []).map((question, questionIndex) => (
                    <article key={question.questionId} className="rounded-2xl border border-[var(--border)] bg-[var(--panel)] p-4">
                      <p className="text-sm font-semibold text-[var(--title)]">
                        {questionIndex + 1}. {question.prompt}
                      </p>

                      <div className="mt-4">
                        <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
                          Resultado
                        </label>
                        {question.type === "number" ? (
                          <input
                            type="number"
                            inputMode="decimal"
                            value={String(question.answer ?? "")}
                            onChange={(event) =>
                              handleQuestionChange(section.sectionId, question.questionId, "answer", event.target.value)
                            }
                            className="field-base mt-2"
                            disabled={!canEdit}
                          />
                        ) : (
                          <select
                            value={String(question.answer ?? "")}
                            onChange={(event) =>
                              handleQuestionChange(section.sectionId, question.questionId, "answer", event.target.value)
                            }
                            className="field-base mt-2"
                            disabled={!canEdit}
                          >
                            <option value="">Sin valor</option>
                            {ENUM_OPTIONS.map((option) => (
                              <option key={option.value} value={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </select>
                        )}
                      </div>
                    </article>
                  ))}
                </div>
              </section>
            ))}

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
                  disabled={!canEdit}
                />
              </div>
            </section>

            <section className="rounded-2xl border border-[var(--border)] bg-white p-5 shadow-[var(--shadow)]">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold text-[var(--title)]">Evidencias fotograficas</h2>
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
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="mt-4 text-sm text-[var(--shell-text)]/80">Sin evidencias adjuntas.</p>
              )}
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

function buildDraftSections(detail) {
  const sectionsMap = detail?.sections ?? {};
  const formAnswersByCode = Object.fromEntries(
    (detail?.formSections ?? []).flatMap((section) =>
      (section.questions ?? []).map((question) => [question.code, question.answer])
    )
  );

  return FORM_SECTION_CONFIG.map((section) => ({
    sectionId: section.key,
    title: section.title,
    questions: section.questions.map((question) => ({
      questionId: question.code,
      code: question.code,
      prompt: question.prompt,
      type: question.type,
      answer: normalizeDraftValue(
        sectionsMap?.[section.key]?.[question.code] ?? formAnswersByCode?.[question.code]
      )
    }))
  }));
}

function normalizeDraftValue(value) {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value);
}

function toSectionsPayload(draftSections) {
  return Object.fromEntries(
    (draftSections ?? []).map((section) => [
      section.sectionId,
      Object.fromEntries(
        (section.questions ?? []).map((question) => [question.code, question.answer === "" ? null : question.answer])
      )
    ])
  );
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

function getFilenameFromDisposition(disposition) {
  if (!disposition) {
    return null;
  }

  const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utfMatch?.[1]) {
    return decodeURIComponent(utfMatch[1]);
  }

  const plainMatch = disposition.match(/filename="?([^"]+)"?/i);
  return plainMatch?.[1] ?? null;
}
