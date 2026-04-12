import { useMemo, useState } from "react";
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

const cedisSchema = z.object({
  name: z.string().min(1, "Nombre es obligatorio"),
  email: z.string().email("Correo invalido"),
  phone: z.string().min(1, "Telefono es obligatorio"),
  alternatePhone: z.string().min(1, "Telefono alternativo es obligatorio"),
  address: z.string(),
  manager: z.string()
});

const defaultValues = {
  name: "",
  email: "",
  phone: "",
  alternatePhone: "",
  address: "",
  manager: ""
};

function digitsOnly(value) {
  return value.replace(/\D/g, "");
}

export function CedisCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [feedbackError, setFeedbackError] = useState(null);
  const form = useForm({
    resolver: zodResolver(cedisSchema),
    defaultValues,
    mode: "onChange"
  });

  const createMutation = useMutation({
    mutationFn: (values) => api.post("/admin/cedis", values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["cedis"] });
      setFlashMessage("CEDIS registrado correctamente");
      await navigate({ to: "/cedis" });
    },
    onError: (error) => {
      console.error(error);
      setFeedbackError("Error al registrar el CEDIS");
    }
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setFeedbackError(null);
    const { address: _address, manager: _manager, ...payload } = values;
    await createMutation.mutateAsync(payload);
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

  return (
    <div className="space-y-6">
      <AlertMessage message={feedbackError} />

      <PagePanel>
        <PageTitleBar
          title="Nuevo CEDIS"
          subtitle="Captura la informacion necesaria para registrar un nuevo CEDIS."
          actions={
            <div className="flex flex-col gap-3 sm:flex-row">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/cedis" })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton type="button" onClick={() => void onSubmit()} disabled={!form.formState.isValid || createMutation.isPending}>
                Guardar
              </PrimaryActionButton>
            </div>
          }
        />

        <div className="grid gap-5 px-5 py-5 md:grid-cols-2">
          <FormField label="Nombre" error={form.formState.errors.name?.message} input={<input {...form.register("name")} type="text" className="field-base" />} />
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
          <FormField label="Direccion" error={form.formState.errors.address?.message} input={<input {...form.register("address")} type="text" className="field-base" />} />
          <FormField label="Encargado" error={form.formState.errors.manager?.message} input={<input {...form.register("manager")} type="text" className="field-base" />} />
        </div>
      </PagePanel>
    </div>
  );
}

export function CedisDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const cedisId = String(params.id ?? "");

  const cedisQuery = useQuery({
    queryKey: ["cedis", cedisId],
    queryFn: () => api.get(`/admin/cedis/${cedisId}`),
    enabled: Boolean(cedisId)
  });

  const details = useMemo(
    () => [
      { label: "Nombre", value: cedisQuery.data?.name ?? "-" },
      { label: "Correo", value: cedisQuery.data?.email ?? "-" },
      { label: "Telefono", value: cedisQuery.data?.phone ?? "-" },
      { label: "Telefono alternativo", value: cedisQuery.data?.alternatePhone ?? "-" }
    ],
    [cedisQuery.data]
  );

  if (cedisQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Detalle de CEDIS" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando informacion del CEDIS...</div>
        </PagePanel>
      </div>
    );
  }

  if (cedisQuery.isError || !cedisQuery.data) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Detalle de CEDIS"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/cedis" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="No se pudo cargar la informacion del CEDIS"
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
          title="Detalle de CEDIS"
          subtitle="Consulta la informacion registrada del CEDIS seleccionado."
          actions={
            <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/cedis" })}>
              Volver
            </SecondaryActionButton>
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
