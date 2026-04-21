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

const OPT = {
  generalLight: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LEFT_BURNT", label: "Lado izquierdo quemado" },
    { value: "RIGHT_BURNT", label: "Lado derecho quemado" },
    { value: "BOTH_BURNT", label: "Ambos quemados" }
  ],
  indicators: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "ONE_BURNT", label: "Una quemada" },
    { value: "TWO_BURNT", label: "Dos quemadas" },
    { value: "THREE_BURNT", label: "Tres quemadas" }
  ],
  headlight: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LOOSE", label: "Suelto" },
    { value: "BROKEN", label: "Roto" }
  ],
  rines: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LEFT_DAMAGED", label: "Rin izquierdo danado" },
    { value: "RIGHT_DAMAGED", label: "Rin derecho danado" },
    { value: "BOTH_DAMAGED", label: "Ambos danados" }
  ],
  masas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LEFT_LEAK", label: "Fuga izquierda" },
    { value: "RIGHT_LEAK", label: "Fuga derecha" },
    { value: "BOTH_LEAK", label: "Ambas fugas" }
  ],
  tuercas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "BROKEN", label: "Rotas" },
    { value: "MISSING", label: "Faltantes" }
  ],
  brazoPitman: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "HIT", label: "Golpeado" }
  ],
  manijasPuertas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "ONE_BROKEN", label: "Una rota" },
    { value: "TWO_BROKEN", label: "Dos rotas" }
  ],
  chavetas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "MISSING", label: "Faltantes" }
  ],
  compresor: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "NO_CUT", label: "Sin corte" },
    { value: "FAILED", label: "Falla" }
  ],
  tanquesAire: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "FAILED", label: "Falla" }
  ],
  motorEmisiones: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "FAILED", label: "Falla" }
  ],
  cajaDireccion: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LEAK", label: "Fuga" }
  ],
  depositoAceite: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "LEAK", label: "Fuga" }
  ],
  parabrisas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "CRACKED", label: "Quebrado" }
  ],
  limpiaparabrisas: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "ONE_BLADE_MISSING", label: "Una pluma faltante" },
    { value: "TWO_BLADES_MISSING", label: "Dos plumas faltantes" },
    { value: "NOT_WORKING", label: "No funciona" }
  ],
  juego: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "FAILED", label: "Falla" }
  ],
  escape: [
    { value: "APPROVED", label: "Aprobado" },
    { value: "MISSING", label: "Faltante" },
    { value: "BROKEN", label: "Roto" }
  ]
};

const FORM_SECTION_CONFIG = [
  {
    key: "luces",
    title: "Luces",
    questions: [
      { code: "luces_galibo", prompt: "Luces galibo", options: OPT.generalLight },
      { code: "luces_altas", prompt: "Luces altas", options: OPT.generalLight },
      { code: "luces_bajas", prompt: "Luces bajas", options: OPT.generalLight },
      { code: "luces_demarcadoras_delanteras", prompt: "Luces demarcadoras delanteras", options: OPT.generalLight },
      { code: "luces_demarcadoras_traseras", prompt: "Luces demarcadoras traseras", options: OPT.generalLight },
      { code: "luces_indicadoras", prompt: "Luces indicadoras", options: OPT.indicators },
      { code: "faro_izquierdo", prompt: "Faro izquierdo", options: OPT.headlight },
      { code: "faro_derecho", prompt: "Faro derecho", options: OPT.headlight },
      { code: "luces_direccionales_delanteras", prompt: "Luces direccionales delanteras", options: OPT.generalLight },
      { code: "luces_direccionales_traseras", prompt: "Luces direccionales traseras", options: OPT.generalLight }
    ]
  },
  {
    key: "llantas",
    title: "Llantas",
    birlosEvaluators: [
      {
        key: "delantera_izquierda",
        title: "Delantera izquierda",
        countCode: "llantas_birlos_delantera_izquierda_count",
        selectedCode: "llantas_birlos_delantera_izquierda_selected"
      },
      {
        key: "delantera_derecha",
        title: "Delantera derecha",
        countCode: "llantas_birlos_delantera_derecha_count",
        selectedCode: "llantas_birlos_delantera_derecha_selected"
      },
      {
        key: "trasera_izquierda",
        title: "Trasera izquierda",
        countCode: "llantas_birlos_trasera_izquierda_count",
        selectedCode: "llantas_birlos_trasera_izquierda_selected"
      },
      {
        key: "trasera_derecha",
        title: "Trasera derecha",
        countCode: "llantas_birlos_trasera_derecha_count",
        selectedCode: "llantas_birlos_trasera_derecha_selected"
      },
      {
        key: "media_izquierda",
        title: "Media izquierda",
        countCode: "llantas_birlos_media_izquierda_count",
        selectedCode: "llantas_birlos_media_izquierda_selected"
      },
      {
        key: "media_derecha",
        title: "Media derecha",
        countCode: "llantas_birlos_media_derecha_count",
        selectedCode: "llantas_birlos_media_derecha_selected"
      }
    ],
    questions: [
      { code: "llantas_rines_delanteros", prompt: "Llantas rines delanteros", options: OPT.rines },
      { code: "llantas_rines_traseros", prompt: "Llantas rines traseros", options: OPT.rines },
      { code: "llantas_masas_delanteras", prompt: "Llantas masas delanteras", options: OPT.masas },
      { code: "llantas_masas_traseras", prompt: "Llantas masas traseras", options: OPT.masas },
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
      { code: "llantas_tuercas_delantera_izquierda", prompt: "Llantas tuercas delantera izquierda", options: OPT.tuercas },
      { code: "llantas_tuercas_delantera_izquierda_faltantes", prompt: "Tuercas faltantes delantera izquierda", type: "number" },
      { code: "llantas_tuercas_delantera_izquierda_rotas", prompt: "Tuercas rotas delantera izquierda", type: "number" },
      { code: "llantas_tuercas_delantera_derecha", prompt: "Llantas tuercas delantera derecha", options: OPT.tuercas },
      { code: "llantas_tuercas_delantera_derecha_faltantes", prompt: "Tuercas faltantes delantera derecha", type: "number" },
      { code: "llantas_tuercas_delantera_derecha_rotas", prompt: "Tuercas rotas delantera derecha", type: "number" },
      { code: "llantas_tuercas_trasera_izquierda", prompt: "Llantas tuercas trasera izquierda", options: OPT.tuercas },
      { code: "llantas_tuercas_trasera_izquierda_faltantes", prompt: "Tuercas faltantes trasera izquierda", type: "number" },
      { code: "llantas_tuercas_trasera_izquierda_rotas", prompt: "Tuercas rotas trasera izquierda", type: "number" },
      { code: "llantas_tuercas_trasera_derecha", prompt: "Llantas tuercas trasera derecha", options: OPT.tuercas },
      { code: "llantas_tuercas_trasera_derecha_faltantes", prompt: "Tuercas faltantes trasera derecha", type: "number" },
      { code: "llantas_tuercas_trasera_derecha_rotas", prompt: "Tuercas rotas trasera derecha", type: "number" }
    ]
  },
  {
    key: "direccion",
    title: "Direccion, estructura y accesos",
    questions: [
      { code: "direccion_brazo_pitman", prompt: "Brazo pitman", options: OPT.brazoPitman },
      { code: "direccion_manijas_puertas", prompt: "Manijas de puertas", options: OPT.manijasPuertas },
      { code: "direccion_chavetas", prompt: "Chavetas", options: OPT.chavetas },
      { code: "direccion_chavetas_faltantes", prompt: "En caso de que hagan falta chavetas", type: "number" }
    ]
  },
  {
    key: "aire_frenos",
    title: "Sistema de aire / frenos",
    questions: [
      { code: "aire_frenos_compresor", prompt: "Compresor", options: OPT.compresor },
      { code: "aire_frenos_tanques_aire", prompt: "Tanques de aire", options: OPT.tanquesAire },
      { code: "aire_frenos_tiempo_carga_psi", prompt: "Tiempo de carga psi", type: "number" },
      { code: "aire_frenos_tiempo_carga_tiempo", prompt: "Tiempo de carga tiempo", type: "number" }
    ]
  },
  {
    key: "motor_emisiones",
    title: "Motor y emisiones",
    questions: [
      { code: "motor_emisiones_humo", prompt: "Humo", options: OPT.motorEmisiones },
      { code: "motor_emisiones_gobernado", prompt: "Gobernado", options: OPT.motorEmisiones }
    ]
  },
  {
    key: "otros",
    title: "Otros",
    questions: [
      { code: "otros_caja_direccion", prompt: "Caja direccion", options: OPT.cajaDireccion },
      { code: "otros_deposito_aceite", prompt: "Deposito aceite", options: OPT.depositoAceite },
      { code: "otros_parabrisas", prompt: "Parabrisas", options: OPT.parabrisas },
      { code: "otros_limpiaparabrisas", prompt: "Limpiaparabrisas", options: OPT.limpiaparabrisas },
      { code: "otros_juego", prompt: "Huelgo", options: OPT.juego },
      { code: "otros_escape", prompt: "Escape", options: OPT.escape }
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

  const handleBirlosCountChange = (sectionId, evaluatorKey, value) => {
    const nextCount = Number.parseInt(value, 10);
    setDraftFormSections((current) =>
      current.map((section) => {
        if (section.sectionId !== sectionId) {
          return section;
        }

        return {
          ...section,
          birlosEvaluators: (section.birlosEvaluators ?? []).map((evaluator) => {
            if (evaluator.key !== evaluatorKey) {
              return evaluator;
            }

            const normalizedCount = Number.isNaN(nextCount) ? 0 : Math.min(Math.max(nextCount, 0), 8);
            return {
              ...evaluator,
              count: normalizedCount,
              selected: evaluator.selected.filter((index) => index <= normalizedCount)
            };
          })
        };
      })
    );
  };

  const handleBirloToggle = (sectionId, evaluatorKey, birloIndex) => {
    setDraftFormSections((current) =>
      current.map((section) => {
        if (section.sectionId !== sectionId) {
          return section;
        }

        return {
          ...section,
          birlosEvaluators: (section.birlosEvaluators ?? []).map((evaluator) => {
            if (evaluator.key !== evaluatorKey || birloIndex > evaluator.count) {
              return evaluator;
            }

            const selectedSet = new Set(evaluator.selected);
            if (selectedSet.has(birloIndex)) {
              selectedSet.delete(birloIndex);
            } else {
              selectedSet.add(birloIndex);
            }

            return {
              ...evaluator,
              selected: Array.from(selectedSet).sort((left, right) => left - right)
            };
          })
        };
      })
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
                  <StatusChip
                    label={buildSectionSummaryLabel(section)}
                    tone={getSectionTone(section)}
                  />
                </div>

                <div className="mt-5 space-y-4">
                  {(section.birlosEvaluators?.length ?? 0) > 0 ? (
                    <div className="grid gap-4 xl:grid-cols-2">
                      {section.birlosEvaluators.map((evaluator) => (
                        <BirlosEvaluatorCard
                          key={evaluator.key}
                          evaluator={evaluator}
                          disabled={!canEdit}
                          onCountChange={(value) => handleBirlosCountChange(section.sectionId, evaluator.key, value)}
                          onToggleBirlo={(birloIndex) => handleBirloToggle(section.sectionId, evaluator.key, birloIndex)}
                        />
                      ))}
                    </div>
                  ) : null}

                  {(section.questions ?? []).map((question, questionIndex) => (
                    <article key={question.questionId} className="rounded-2xl border border-[var(--border)] bg-[var(--panel)] p-4">
                      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                        <p className="text-sm font-semibold text-[var(--title)]">
                          {questionIndex + 1}. {question.prompt}
                        </p>
                        <StatusChip
                          label={getQuestionRuleStatus(question).label}
                          tone={getQuestionRuleStatus(question).tone}
                        />
                      </div>

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
                        <EvidencePreview evidence={evidence} />
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

function BirlosEvaluatorCard({ evaluator, disabled, onCountChange, onToggleBirlo }) {
  const wheelPoints = buildWheelPoints(Math.max(evaluator.count, 1));
  const birlosStatus = getBirlosRuleStatus(evaluator);

  return (
    <article className="rounded-2xl border border-[var(--border)] bg-[var(--panel)] p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-[var(--title)]">{evaluator.title}</p>
          <p className="mt-1 text-xs text-[var(--shell-text)]/75">
            Marca cada birlo presente haciendo clic sobre el dibujo de la llanta.
          </p>
          <div className="mt-3">
            <StatusChip label={birlosStatus.label} tone={birlosStatus.tone} />
          </div>
        </div>

        <div className="sm:w-40">
          <label className="text-xs font-bold uppercase tracking-[0.06em] text-[var(--shell-text)]/75">
            Cantidad de birlos
          </label>
          <input
            type="number"
            min="0"
            max="8"
            value={String(evaluator.count ?? 0)}
            onChange={(event) => onCountChange(event.target.value)}
            className="field-base mt-2"
            disabled={disabled}
          />
        </div>
      </div>

      <div className="mt-4 flex flex-col items-center gap-4 rounded-2xl bg-white px-4 py-5">
        <div className="birlos-wheel">
          <div className="birlos-wheel__tire" />
          <div className="birlos-wheel__rim" />
          <div className="birlos-wheel__hub" />
          {wheelPoints.map((point) => {
            const isSelected = evaluator.selected.includes(point.index);
            return (
              <button
                key={point.index}
                type="button"
                className={`birlos-wheel__bolt ${isSelected ? "is-selected" : ""}`}
                style={{ left: `${point.left}%`, top: `${point.top}%` }}
                onClick={() => onToggleBirlo(point.index)}
                disabled={disabled || evaluator.count === 0}
                aria-pressed={isSelected}
                aria-label={`Birlo ${point.index}`}
              >
                {point.index}
              </button>
            );
          })}
        </div>

        <div className="w-full rounded-xl border border-[var(--border)] bg-[var(--panel)] px-3 py-2 text-sm text-[var(--shell-text)]">
          {evaluator.selected.length > 0 ? (
            <>Birlos marcados: {evaluator.selected.join(", ")}</>
          ) : (
            "Sin birlos marcados."
          )}
        </div>
      </div>
    </article>
  );
}

function EvidencePreview({ evidence }) {
  const [failed, setFailed] = useState(false);
  const isImage = evidence.mimeType?.startsWith("image/");
  const canRenderInline = isImage && !/heic|heif/i.test(String(evidence.mimeType ?? ""));

  if (!canRenderInline || failed) {
    return (
      <a
        href={evidence.previewUrl}
        target="_blank"
        rel="noreferrer"
        className="flex h-52 items-center justify-center bg-slate-100 px-4 text-center text-sm font-semibold text-[var(--title)]"
      >
        Abrir imagen
      </a>
    );
  }

  return (
    <a href={evidence.previewUrl} target="_blank" rel="noreferrer" className="block">
      <img
        src={evidence.previewUrl}
        alt={evidence.filename}
        className="h-52 w-full bg-slate-100 object-cover"
        loading="lazy"
        onError={() => setFailed(true)}
      />
    </a>
  );
}

function buildDraftSections(detail) {
  const sectionsMap = detail?.sections ?? {};
  const formAnswersByCode = Object.fromEntries(
    (detail?.formSections ?? []).flatMap((section) =>
      (section.questions ?? []).flatMap((question) => {
        const aliases = QUESTION_CODE_ALIASES[question.code] ?? [];
        return [[question.code, question], ...aliases.map((alias) => [alias, question])];
      })
    )
  );

  return FORM_SECTION_CONFIG.map((section) => ({
    sectionId: section.key,
    title: section.title,
    birlosEvaluators: (section.birlosEvaluators ?? []).map((evaluator) => ({
      key: evaluator.key,
      title: evaluator.title,
      countCode: evaluator.countCode,
      selectedCode: evaluator.selectedCode,
      count: parseBirlosCount(sectionsMap?.[section.key]?.[evaluator.countCode]),
      selected: parseBirlosSelection(sectionsMap?.[section.key]?.[evaluator.selectedCode])
    })),
    questions: section.questions.map((question) => ({
      questionId: question.code,
      code: question.code,
      prompt: question.prompt,
      type: question.type,
      answer: normalizeDraftValue({
        value: sectionsMap?.[section.key]?.[question.code],
        fallbackQuestion: formAnswersByCode?.[question.code],
        type: question.type
      })
    }))
  }));
}

const QUESTION_CODE_ALIASES = {
  llantas_tuercas_delantera_izquierda_faltantes: ["llantas_tuercas_faltantes_delantera_izquierda"],
  llantas_tuercas_delantera_izquierda_rotas: ["llantas_tuercas_rotas_delantera_izquierda"],
  llantas_tuercas_delantera_derecha_faltantes: ["llantas_tuercas_faltantes_delantera_derecha"],
  llantas_tuercas_delantera_derecha_rotas: ["llantas_tuercas_rotas_delantera_derecha"],
  llantas_tuercas_trasera_izquierda_faltantes: ["llantas_tuercas_faltantes_trasera_izquierda"],
  llantas_tuercas_trasera_izquierda_rotas: ["llantas_tuercas_rotas_trasera_izquierda"],
  llantas_tuercas_trasera_derecha_faltantes: ["llantas_tuercas_faltantes_trasera_derecha"],
  llantas_tuercas_trasera_derecha_rotas: ["llantas_tuercas_rotas_trasera_derecha"]
};

function normalizeDraftValue({ value, fallbackQuestion, type }) {
  if (value !== null && value !== undefined && value !== "") {
    return String(value);
  }

  if (!fallbackQuestion) {
    return "";
  }

  if (type === "number") {
    return fallbackQuestion.comment ?? "";
  }

  return normalizeMobileEnumValue(fallbackQuestion.answer);
}

function normalizeMobileEnumValue(value) {
  switch (String(value ?? "").toUpperCase()) {
    case "PASS":
      return "APROBADO";
    case "FAIL":
      return "REPROBADO";
    case "NA":
      return "NO_APLICA";
    default:
      return value ? String(value) : "";
  }
}

function toSectionsPayload(draftSections) {
  return Object.fromEntries(
    (draftSections ?? []).map((section) => [
      section.sectionId,
      {
        ...Object.fromEntries(
          (section.questions ?? []).map((question) => [question.code, question.answer === "" ? null : question.answer])
        ),
        ...Object.fromEntries(
          (section.birlosEvaluators ?? []).flatMap((evaluator) => [
            [evaluator.countCode, evaluator.count > 0 ? String(evaluator.count) : null],
            [evaluator.selectedCode, evaluator.selected.length > 0 ? evaluator.selected.join(",") : null]
          ])
        )
      }
    ])
  );
}

function parseBirlosCount(value) {
  const numericValue = Number.parseInt(String(value ?? ""), 10);
  return Number.isNaN(numericValue) ? 0 : Math.min(Math.max(numericValue, 0), 8);
}

function parseBirlosSelection(value) {
  return String(value ?? "")
    .split(",")
    .map((item) => Number.parseInt(item.trim(), 10))
    .filter((item) => !Number.isNaN(item) && item >= 1 && item <= 8)
    .sort((left, right) => left - right);
}

function getQuestionRuleStatus(question) {
  const value = Number.parseFloat(String(question.answer ?? "").replace(",", "."));
  const hasNumericValue = question.type === "number" && !Number.isNaN(value);

  if (question.type !== "number" || !question.answer) {
    return { label: "Pendiente", tone: "neutral" };
  }

  if (!hasNumericValue) {
    return { label: "Pendiente", tone: "neutral" };
  }

  if (question.code.startsWith("llantas_presion_")) {
    return value >= 80 ? { label: "Cumple PSI", tone: "success" } : { label: "No cumple PSI", tone: "danger" };
  }

  if (
    question.code === "llantas_profundidad_delantera_izquierda" ||
    question.code === "llantas_profundidad_delantera_derecha"
  ) {
    return value >= 3.2 ? { label: "Cumple profundidad", tone: "success" } : { label: "No cumple profundidad", tone: "danger" };
  }

  if (question.code.startsWith("llantas_profundidad_trasera_")) {
    return value >= 1.6 ? { label: "Cumple profundidad", tone: "success" } : { label: "No cumple profundidad", tone: "danger" };
  }

  if (question.code === "aire_frenos_tiempo_carga_psi") {
    return value >= 70 && value <= 120
      ? { label: "Cumple rango PSI", tone: "success" }
      : { label: "Fuera de rango PSI", tone: "danger" };
  }

  if (question.code === "aire_frenos_tiempo_carga_tiempo") {
    return value < 120 ? { label: "Cumple tiempo", tone: "success" } : { label: "Excede tiempo", tone: "danger" };
  }

  return { label: "Capturado", tone: "neutral" };
}

function getBirlosRuleStatus(evaluator) {
  if (!evaluator.count) {
    return { label: "Pendiente", tone: "neutral" };
  }

  const missing = Math.max(evaluator.count - evaluator.selected.length, 0);
  return missing > 2
    ? { label: `Reprueba: faltan ${missing} birlos`, tone: "danger" }
    : { label: `Cumple: faltan ${missing} birlos`, tone: "success" };
}

function getSectionTone(section) {
  const failingQuestion = (section.questions ?? []).some((question) => getQuestionRuleStatus(question).tone === "danger");
  const failingBirlos = (section.birlosEvaluators ?? []).some((evaluator) => getBirlosRuleStatus(evaluator).tone === "danger");
  if (failingQuestion || failingBirlos) {
    return "danger";
  }
  const pendingQuestion = (section.questions ?? []).some((question) => getQuestionRuleStatus(question).tone === "neutral");
  const pendingBirlos = (section.birlosEvaluators ?? []).some((evaluator) => getBirlosRuleStatus(evaluator).tone === "neutral");
  return pendingQuestion || pendingBirlos ? "neutral" : "success";
}

function buildSectionSummaryLabel(section) {
  const totalChecks = (section.questions?.length ?? 0) + (section.birlosEvaluators?.length ?? 0);
  const failedChecks =
    (section.questions ?? []).filter((question) => getQuestionRuleStatus(question).tone === "danger").length +
    (section.birlosEvaluators ?? []).filter((evaluator) => getBirlosRuleStatus(evaluator).tone === "danger").length;
  if (failedChecks > 0) {
    return `${failedChecks} de ${totalChecks} fuera de criterio`;
  }
  return `${totalChecks} criterio${totalChecks === 1 ? "" : "s"} en revision`;
}

function buildWheelPoints(count) {
  return Array.from({ length: count }, (_, index) => {
    const angle = (Math.PI * 2 * index) / count - Math.PI / 2;
    const radius = 34;
    return {
      index: index + 1,
      left: 50 + Math.cos(angle) * radius,
      top: 50 + Math.sin(angle) * radius
    };
  });
}

function formatSectionName(value) {
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
