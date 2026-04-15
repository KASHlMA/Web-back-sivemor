import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import {
  AlertMessage,
  EmptyState,
  FieldError,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton
} from "../../components/AdminPrimitives";
import { schemaHelpers } from "../../components/ResourceTablePage";
import { api } from "../../lib/api";
import { setFlashMessage } from "../../lib/flashMessage";

const verificationCenterSchema = z.object({
  name: schemaHelpers.requiredText("Nombre"),
  centerKey: schemaHelpers.requiredText("Clave de verificentro"),
  address: schemaHelpers.requiredText("Dirección"),
  regionId: z.string().min(1, "Región es obligatoria"),
  manager: schemaHelpers.requiredText("Responsable"),
  email: schemaHelpers.email("Correo"),
  phone: schemaHelpers.phone("Teléfono"),
  alternatePhone: schemaHelpers.phone("Teléfono alternativo"),
  schedule: schemaHelpers.requiredText("Horario")
});

const defaultValues = {
  name: "",
  centerKey: "",
  address: "",
  regionId: "",
  manager: "",
  email: "",
  phone: "",
  alternatePhone: "",
  schedule: ""
};

function digitsOnly(value) {
  return value.replace(/\D/g, "");
}

export function VerificationCenterCreatePage() {
  return <VerificationCenterFormPage mode="create" />;
}

export function VerificationCenterEditPage() {
  return <VerificationCenterFormPage mode="edit" />;
}

function VerificationCenterFormPage({ mode }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams({ strict: false });
  const verificationCenterId = mode === "edit" ? String(params.id ?? "") : "";
  const [feedbackError, setFeedbackError] = useState(null);

  const regionsQuery = useQuery({
    queryKey: ["regions-lookup"],
    queryFn: () => api.get("/admin/regions")
  });

  const verificationCenterQuery = useQuery({
    queryKey: ["verification-centers", verificationCenterId],
    queryFn: () => api.get(`/admin/verification-centers/${verificationCenterId}`),
    enabled: mode === "edit" && Boolean(verificationCenterId)
  });

  const form = useForm({
    resolver: zodResolver(verificationCenterSchema),
    defaultValues,
    mode: "onChange"
  });

  const verificationCenterRecord = verificationCenterQuery.data ?? null;

  const regionOptions = useMemo(
    () =>
      (regionsQuery.data ?? []).map((item) => ({
        label: item.name,
        value: String(item.id)
      })),
    [regionsQuery.data]
  );

  const saveSuccessMessage =
    mode === "edit" ? "Verificentro actualizado correctamente" : "Verificentro registrado correctamente";
  const saveErrorMessage =
    mode === "edit" ? "Error al actualizar el verificentro" : "Error al registrar el verificentro";

  const saveMutation = useMutation({
    mutationFn: (values) => {
      const payload = {
        ...values,
        regionId: Number(values.regionId)
      };

      if (mode === "edit") {
        return api.put(`/admin/verification-centers/${verificationCenterId}`, payload);
      }

      return api.post("/admin/verification-centers", payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["verification-centers"] });
      setFlashMessage(saveSuccessMessage);
      await navigate({ to: "/verification-centers" });
    },
    onError: (error) => {
      setFeedbackError(error instanceof Error ? error.message : saveErrorMessage);
    }
  });

  useEffect(() => {
    if (mode !== "edit" || !verificationCenterRecord) {
      return;
    }

    form.reset({
      name: verificationCenterRecord.name,
      centerKey: verificationCenterRecord.centerKey,
      address: verificationCenterRecord.address,
      regionId: String(verificationCenterRecord.regionId),
      manager: verificationCenterRecord.manager,
      email: verificationCenterRecord.email,
      phone: verificationCenterRecord.phone,
      alternatePhone: verificationCenterRecord.alternatePhone,
      schedule: verificationCenterRecord.schedule
    });
  }, [form, mode, verificationCenterRecord]);

  if (mode === "edit" && verificationCenterQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Editar verificentro" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">{"Cargando informaci\u00f3n del verificentro..."}</div>
        </PagePanel>
      </div>
    );
  }

  if (mode === "edit" && (verificationCenterQuery.isError || !verificationCenterRecord)) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Editar verificentro"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/verification-centers" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title={"No se pudo cargar la informaci\u00f3n del verificentro"}
            description="Verifica que el registro exista e intenta nuevamente."
          />
        </PagePanel>
      </div>
    );
  }

  const phoneField = form.register("phone", {
    onChange: (event) => {
      event.target.value = digitsOnly(event.target.value);
    }
  });

  const alternatePhoneField = form.register("alternatePhone", {
    onChange: (event) => {
      event.target.value = digitsOnly(event.target.value);
    }
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setFeedbackError(null);
    await saveMutation.mutateAsync(values);
  });

  return (
    <div className="space-y-6">
      <AlertMessage message={feedbackError} />

      <PagePanel>
        <PageTitleBar
          title={mode === "edit" ? "Editar verificentro" : "Nuevo verificentro"}
          subtitle={
            mode === "edit"
              ? "Actualiza la informaci\u00f3n registrada del verificentro."
              : "Captura la informaci\u00f3n necesaria para registrar un nuevo verificentro."
          }
          actions={
            <div className="action-group">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/verification-centers" })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton
                type="button"
                onClick={() => void onSubmit()}
                disabled={!form.formState.isValid || saveMutation.isPending}
              >
                Guardar
              </PrimaryActionButton>
            </div>
          }
        />

        <div className="grid gap-5 px-5 py-5 md:grid-cols-2">
          <FormField label="Nombre" error={form.formState.errors.name?.message} input={<input {...form.register("name")} type="text" className="field-base" />} />
          <FormField label="Clave de verificentro" error={form.formState.errors.centerKey?.message} input={<input {...form.register("centerKey")} type="text" className="field-base" />} />
          <FormField label={"Direcci\u00f3n"} error={form.formState.errors.address?.message} input={<input {...form.register("address")} type="text" className="field-base" />} />
          <FormField
            label={"Regi\u00f3n"}
            error={form.formState.errors.regionId?.message}
            input={
              <select {...form.register("regionId")} className="field-base">
                <option value="">{"Selecciona una regi\u00f3n"}</option>
                {regionOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            }
          />
          <FormField label="Responsable" error={form.formState.errors.manager?.message} input={<input {...form.register("manager")} type="text" className="field-base" />} />
          <FormField label="Correo" error={form.formState.errors.email?.message} input={<input {...form.register("email")} type="text" className="field-base" />} />
          <FormField
            label={"Tel\u00e9fono"}
            error={form.formState.errors.phone?.message}
            input={<input {...phoneField} type="text" inputMode="numeric" className="field-base" />}
          />
          <FormField
            label={"Tel\u00e9fono alternativo"}
            error={form.formState.errors.alternatePhone?.message}
            input={<input {...alternatePhoneField} type="text" inputMode="numeric" className="field-base" />}
          />
          <FormField label="Horario" error={form.formState.errors.schedule?.message} input={<input {...form.register("schedule")} type="text" className="field-base" />} />
        </div>
      </PagePanel>
    </div>
  );
}

export function VerificationCenterDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const verificationCenterId = String(params.id ?? "");

  const verificationCenterQuery = useQuery({
    queryKey: ["verification-centers", verificationCenterId],
    queryFn: () => api.get(`/admin/verification-centers/${verificationCenterId}`),
    enabled: Boolean(verificationCenterId)
  });

  const details = useMemo(
    () => [
      { label: "Nombre", value: verificationCenterQuery.data?.name ?? "-" },
      { label: "Clave de verificentro", value: verificationCenterQuery.data?.centerKey ?? "-" },
      { label: "Direcci\u00f3n", value: verificationCenterQuery.data?.address ?? "-" },
      { label: "Regi\u00f3n", value: verificationCenterQuery.data?.regionName ?? "-" },
      { label: "Responsable", value: verificationCenterQuery.data?.manager ?? "-" },
      { label: "Correo", value: verificationCenterQuery.data?.email ?? "-" },
      { label: "Tel\u00e9fono", value: verificationCenterQuery.data?.phone ?? "-" },
      { label: "Tel\u00e9fono alternativo", value: verificationCenterQuery.data?.alternatePhone ?? "-" },
      { label: "Horario", value: verificationCenterQuery.data?.schedule ?? "-" }
    ],
    [verificationCenterQuery.data]
  );

  if (verificationCenterQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
        <PageTitleBar title="Detalle de verificentro" />
        <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">{"Cargando informaci\u00f3n del verificentro..."}</div>
        </PagePanel>
      </div>
    );
  }

  if (verificationCenterQuery.isError || !verificationCenterQuery.data) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Detalle de verificentro"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/verification-centers" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title={"No se pudo cargar la informaci\u00f3n del verificentro"}
            description="Verifica que el registro exista e intenta nuevamente."
          />
        </PagePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PagePanel>
        <PageTitleBar
          title="Detalle de verificentro"
          subtitle={"Consulta la informaci\u00f3n registrada del verificentro seleccionado."}
          actions={
            <div className="action-group">
              <SecondaryActionButton
                type="button"
                onClick={() =>
                  void navigate({ to: "/verification-centers/$id/editar", params: { id: verificationCenterId } })
                }
              >
            {"Editar informaci\u00f3n"}
              </SecondaryActionButton>
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/verification-centers" })}>
                Volver
              </SecondaryActionButton>
            </div>
          }
        />

        <div className="grid gap-4 px-5 py-5 md:grid-cols-2">
          {details.map((item) => (
            <div key={item.label} className="rounded-xl border border-[var(--border)] bg-[#f8fbf4] px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.04em] text-[var(--shell-text)]/75">{item.label}</p>
              <p className="mt-2 text-base font-semibold text-[var(--title)]">{item.value}</p>
            </div>
          ))}
        </div>
      </PagePanel>
    </div>
  );
}

function FormField({ label, input, error }) {
  return (
    <div>
      <label className="field-label">{label}</label>
      {input}
      <FieldError message={error} />
    </div>
  );
}
