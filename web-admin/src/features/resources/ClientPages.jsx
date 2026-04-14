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
import { api } from "../../lib/api";
import { setFlashMessage } from "../../lib/flashMessage";

const clientSchema = z.object({
  name: z.string().min(1, "Nombre es obligatorio"),
  businessName: z.string().min(1, "Razon social es obligatoria"),
  email: z.string().email("Correo invalido"),
  phone: z.string().min(1, "Telefono es obligatorio"),
  alternatePhone: z.string().min(1, "Telefono alternativo es obligatorio"),
  manager: z.string().min(1, "Gestor es obligatorio")
});

const defaultValues = {
  name: "",
  businessName: "",
  email: "",
  phone: "",
  alternatePhone: "",
  manager: ""
};

function digitsOnly(value) {
  return value.replace(/\D/g, "");
}

export function ClientCreatePage() {
  return <ClientFormPage mode="create" />;
}

export function ClientEditPage() {
  return <ClientFormPage mode="edit" />;
}

function ClientFormPage({ mode }) {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const queryClient = useQueryClient();
  const [feedbackError, setFeedbackError] = useState(null);
  const clientId = mode === "edit" ? String(params.id ?? "") : "";
  const clientQuery = useQuery({
    queryKey: ["clients", clientId],
    queryFn: () => api.get(`/admin/clients/${clientId}`),
    enabled: mode === "edit" && Boolean(clientId)
  });
  const form = useForm({
    resolver: zodResolver(clientSchema),
    defaultValues,
    mode: "onChange"
  });

  const successMessage = mode === "edit" ? "Cliente actualizado correctamente" : "Cliente registrado correctamente";
  const errorMessage = mode === "edit" ? "Error al actualizar el cliente" : "Error al registrar el cliente";

  const createMutation = useMutation({
    mutationFn: (values) => (mode === "edit" ? api.put(`/admin/clients/${clientId}`, values) : api.post("/admin/clients", values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
      await queryClient.invalidateQueries({ queryKey: ["clients-lookup"] });
      await queryClient.invalidateQueries({ queryKey: ["clients", clientId] });
      setFlashMessage(successMessage);
      await navigate({ to: "/clients" });
    },
    onError: (error) => {
      setFeedbackError(error instanceof Error ? error.message : errorMessage);
    }
  });

  useEffect(() => {
    if (mode !== "edit" || !clientQuery.data) {
      return;
    }

    form.reset({
      name: clientQuery.data.name,
      businessName: clientQuery.data.businessName,
      email: clientQuery.data.email,
      phone: clientQuery.data.phone,
      alternatePhone: clientQuery.data.alternatePhone,
      manager: clientQuery.data.manager
    });
  }, [form, mode, clientQuery.data]);

  const onSubmit = form.handleSubmit(async (values) => {
    setFeedbackError(null);
    await createMutation.mutateAsync(values);
  });

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

  if (mode === "edit" && clientQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Editar cliente" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando informacion del cliente...</div>
        </PagePanel>
      </div>
    );
  }

  if (mode === "edit" && (clientQuery.isError || !clientQuery.data)) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Editar cliente"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/clients" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="No se pudo cargar la informacion del cliente"
            description="Verifica que el registro exista e intenta nuevamente."
          />
        </PagePanel>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <AlertMessage message={feedbackError} />

      <PagePanel>
        <PageTitleBar
          title={mode === "edit" ? "Editar cliente" : "Nuevo cliente"}
          subtitle={
            mode === "edit"
              ? "Actualiza la informacion registrada del cliente seleccionado."
              : "Captura la informacion necesaria para registrar un nuevo cliente."
          }
          actions={
            <div className="flex flex-col gap-3 sm:flex-row">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/clients" })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton
                type="button"
                onClick={() => void onSubmit()}
                disabled={!form.formState.isValid || createMutation.isPending}
              >
                Guardar
              </PrimaryActionButton>
            </div>
          }
        />

        <div className="grid gap-5 px-5 py-5 md:grid-cols-2">
          <FormField label="Nombre" error={form.formState.errors.name?.message} input={<input {...form.register("name")} type="text" className="field-base" />} />
          <FormField
            label="Razon social"
            error={form.formState.errors.businessName?.message}
            input={<input {...form.register("businessName")} type="text" className="field-base" />}
          />
          <FormField label="Correo" error={form.formState.errors.email?.message} input={<input {...form.register("email")} type="text" className="field-base" />} />
          <FormField
            label="Telefono"
            error={form.formState.errors.phone?.message}
            input={<input {...phoneField} type="text" inputMode="numeric" className="field-base" />}
          />
          <FormField
            label="Telefono alternativo"
            error={form.formState.errors.alternatePhone?.message}
            input={<input {...alternatePhoneField} type="text" inputMode="numeric" className="field-base" />}
          />
          <FormField label="Gestor" error={form.formState.errors.manager?.message} input={<input {...form.register("manager")} type="text" className="field-base" />} />
        </div>
      </PagePanel>
    </div>
  );
}

export function ClientDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const clientId = String(params.id ?? "");

  const clientQuery = useQuery({
    queryKey: ["clients", clientId],
    queryFn: () => api.get(`/admin/clients/${clientId}`),
    enabled: Boolean(clientId)
  });

  const details = useMemo(
    () => [
      { label: "Nombre", value: clientQuery.data?.name ?? "-" },
      { label: "Razon social", value: clientQuery.data?.businessName ?? "-" },
      { label: "Correo", value: clientQuery.data?.email ?? "-" },
      { label: "Telefono", value: clientQuery.data?.phone ?? "-" },
      { label: "Telefono alternativo", value: clientQuery.data?.alternatePhone ?? "-" },
      { label: "Gestor", value: clientQuery.data?.manager ?? "-" }
    ],
    [clientQuery.data]
  );

  if (clientQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Detalle de cliente" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando informacion del cliente...</div>
        </PagePanel>
      </div>
    );
  }

  if (clientQuery.isError || !clientQuery.data) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Detalle de cliente"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/clients" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="No se pudo cargar la informacion del cliente"
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
          title="Detalle de cliente"
          subtitle="Consulta la informacion registrada del cliente seleccionado."
          actions={
            <div className="flex flex-col gap-3 sm:flex-row">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/clients/$id/editar", params: { id: clientId } })}>
                Editar informacion
              </SecondaryActionButton>
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/clients" })}>
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
