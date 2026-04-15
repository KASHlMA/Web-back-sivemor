import { useEffect, useMemo, useRef, useState } from "react";
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

const vehicleTypeOptions = [
  { label: "N2", value: "N2" },
  { label: "N3", value: "N3" }
];

const vehicleSchema = z.object({
  vin: schemaHelpers.requiredText("Numero de serie"),
  plate: schemaHelpers.requiredText("Placas"),
  category: z.enum(["N2", "N3"], {
    errorMap: () => ({ message: "El tipo es obligatorio" })
  }),
  clientCompanyId: z.string().min(1, "El cliente es obligatorio"),
  regionId: z.string().min(1, "El CEDIS es obligatorio"),
  brand: schemaHelpers.requiredText("Marca"),
  model: schemaHelpers.requiredText("Modelo")
});

const defaultValues = {
  vin: "",
  plate: "",
  category: "",
  clientCompanyId: "",
  regionId: "",
  brand: "",
  model: ""
};

export function VehicleCreatePage() {
  return <VehicleFormPage mode="create" />;
}

export function VehicleEditPage() {
  return <VehicleFormPage mode="edit" />;
}

function VehicleFormPage({ mode }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const params = useParams({ strict: false });
  const vehicleId = mode === "edit" ? Number(params.id) : null;
  const [feedbackError, setFeedbackError] = useState(null);

  const clientsQuery = useQuery({
    queryKey: ["clients-lookup"],
    queryFn: () => api.get("/admin/clients")
  });

  const regionsQuery = useQuery({
    queryKey: ["regions-lookup"],
    queryFn: () => api.get("/admin/regions")
  });

  const vehiclesQuery = useQuery({
    queryKey: ["vehicles-lookup"],
    queryFn: () => api.get("/admin/vehicles"),
    enabled: mode === "edit"
  });

  const form = useForm({
    resolver: zodResolver(vehicleSchema),
    defaultValues,
    mode: "onChange"
  });

  const watchedValues = form.watch();
  const previousClientCompanyIdRef = useRef("");

  const clientOptions = useMemo(
    () =>
      (clientsQuery.data ?? []).map((item) => ({
        label: item.name,
        value: String(item.id)
      })),
    [clientsQuery.data]
  );

  const regionOptions = useMemo(
    () =>
      (regionsQuery.data ?? []).map((item) => ({
        label: item.name,
        value: String(item.id)
      })),
    [regionsQuery.data]
  );

  const selectedClientRegionId = useMemo(() => {
    const selectedClient = (clientsQuery.data ?? []).find(
      (item) => String(item.id) === watchedValues.clientCompanyId
    );
    return selectedClient?.regionId ? String(selectedClient.regionId) : "";
  }, [clientsQuery.data, watchedValues.clientCompanyId]);

  const vehicleRecord = useMemo(() => {
    if (mode !== "edit" || !Number.isFinite(vehicleId)) {
      return null;
    }

    return (vehiclesQuery.data ?? []).find((item) => item.id === vehicleId) ?? null;
  }, [mode, vehicleId, vehiclesQuery.data]);

  const isLoadingVehicleData =
    mode === "edit" && (vehiclesQuery.isLoading || clientsQuery.isLoading || regionsQuery.isLoading);

  const vehicleLoadFailed =
    mode === "edit" &&
    !isLoadingVehicleData &&
    (vehiclesQuery.isError || clientsQuery.isError || regionsQuery.isError || !vehicleRecord);

  const catalogMessages = useMemo(
    () => ({
      category: vehicleTypeOptions.length === 0 ? "No hay tipos disponibles" : null,
      client: !clientsQuery.isLoading && clientOptions.length === 0 ? "No hay clientes disponibles" : null,
      region: !regionsQuery.isLoading && regionOptions.length === 0 ? "No hay CEDIS disponibles" : null
    }),
    [clientOptions.length, clientsQuery.isLoading, regionOptions.length, regionsQuery.isLoading]
  );

  const hasUnavailableCatalog =
    Boolean(catalogMessages.category) || Boolean(catalogMessages.client) || Boolean(catalogMessages.region);

  useEffect(() => {
    if (feedbackError) {
      setFeedbackError(null);
    }
  }, [feedbackError, watchedValues]);

  useEffect(() => {
    if (mode !== "edit" || !vehicleRecord || clientsQuery.isLoading) {
      return;
    }

    const matchedClient = (clientsQuery.data ?? []).find(
      (item) => String(item.id) === String(vehicleRecord.clientCompanyId)
    );

    form.reset({
      vin: vehicleRecord.vin,
      plate: vehicleRecord.plate,
      category: vehicleRecord.category,
      clientCompanyId: String(vehicleRecord.clientCompanyId),
      regionId: matchedClient?.regionId ? String(matchedClient.regionId) : "",
      brand: vehicleRecord.brand,
      model: vehicleRecord.model
    });
    previousClientCompanyIdRef.current = String(vehicleRecord.clientCompanyId);
  }, [clientsQuery.data, clientsQuery.isLoading, form, mode, vehicleRecord]);

  useEffect(() => {
    if (
      !selectedClientRegionId ||
      !watchedValues.clientCompanyId ||
      previousClientCompanyIdRef.current === watchedValues.clientCompanyId
    ) {
      return;
    }

    form.setValue("regionId", selectedClientRegionId, {
      shouldValidate: true,
      shouldDirty: true
    });
    previousClientCompanyIdRef.current = watchedValues.clientCompanyId;
  }, [form, selectedClientRegionId, watchedValues.clientCompanyId]);

  const saveSuccessMessage =
    mode === "edit" ? "Vehiculo actualizado correctamente" : "Vehiculo registrado correctamente";
  const saveErrorMessage =
    mode === "edit" ? "Error al actualizar el vehiculo" : "Error al registrar el vehiculo";

  const saveVehicleMutation = useMutation({
    mutationFn: (values) => {
      const payload = {
        clientCompanyId: Number(values.clientCompanyId),
        plate: values.plate,
        vin: values.vin,
        category: values.category,
        brand: values.brand,
        model: values.model
      };

      if (mode === "edit") {
        return api.put(`/admin/vehicles/${vehicleId}`, payload);
      }

      return api.post("/admin/vehicles", payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["vehicles"] });
      await queryClient.invalidateQueries({ queryKey: ["vehicles-lookup"] });
      setFlashMessage(saveSuccessMessage);
      await navigate({ to: "/vehiculos" });
    },
    onError: () => {
      setFeedbackError(saveErrorMessage);
    }
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setFeedbackError(null);
    await saveVehicleMutation.mutateAsync(values);
  });

  const saveDisabled =
    hasUnavailableCatalog ||
    !form.formState.isValid ||
    saveVehicleMutation.isPending ||
    clientsQuery.isLoading ||
    regionsQuery.isLoading ||
    (mode === "edit" && (isLoadingVehicleData || vehicleLoadFailed));

  if (isLoadingVehicleData) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Editar vehiculo" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">
            Cargando informacion del vehiculo...
          </div>
        </PagePanel>
      </div>
    );
  }

  if (vehicleLoadFailed) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Editar vehiculo" />
          <EmptyState
            title="No se pudo cargar la informacion del vehiculo"
            description="Verifica la disponibilidad de los datos del vehiculo e intenta nuevamente."
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
          title={mode === "edit" ? "Editar vehiculo" : "Nuevo vehiculo"}
          subtitle={
            mode === "edit"
              ? "Actualiza la informacion registrada del vehiculo seleccionado."
              : "Captura la informacion requerida para registrar un nuevo vehiculo en el sistema."
          }
          actions={
            <div className="flex flex-col gap-3 sm:flex-row">
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/vehiculos" })}>
                Volver
              </SecondaryActionButton>
              <PrimaryActionButton type="button" onClick={() => void onSubmit()} disabled={saveDisabled}>
                Guardar
              </PrimaryActionButton>
            </div>
          }
        />

        <div className="grid gap-5 px-5 py-5 md:grid-cols-2">
          <FormField
            label="Numero de serie"
            error={form.formState.errors.vin?.message}
            input={<input {...form.register("vin")} type="text" className="field-base" />}
          />

          <FormField
            label="Placas"
            error={form.formState.errors.plate?.message}
            input={<input {...form.register("plate")} type="text" className="field-base" />}
          />

          <SelectField
            label="Tipo"
            error={form.formState.errors.category?.message}
            message={catalogMessages.category}
            disabled={vehicleTypeOptions.length === 0}
            value={watchedValues.category}
            onChange={(event) =>
              form.setValue("category", event.target.value, {
                shouldValidate: true,
                shouldDirty: true
              })
            }
            options={vehicleTypeOptions}
          />

          <SelectField
            label="Cliente"
            error={form.formState.errors.clientCompanyId?.message}
            message={catalogMessages.client}
            disabled={clientsQuery.isLoading || clientOptions.length === 0}
            value={watchedValues.clientCompanyId}
            onChange={(event) =>
              form.setValue("clientCompanyId", event.target.value, {
                shouldValidate: true,
                shouldDirty: true
              })
            }
            options={clientOptions}
          />

          <SelectField
            label="CEDIS"
            error={form.formState.errors.regionId?.message}
            message={catalogMessages.region}
            disabled={regionsQuery.isLoading || regionOptions.length === 0}
            value={watchedValues.regionId}
            onChange={(event) =>
              form.setValue("regionId", event.target.value, {
                shouldValidate: true,
                shouldDirty: true
              })
            }
            options={regionOptions}
          />

          <FormField
            label="Marca"
            error={form.formState.errors.brand?.message}
            input={<input {...form.register("brand")} type="text" className="field-base" />}
          />

          <FormField
            label="Modelo"
            error={form.formState.errors.model?.message}
            input={<input {...form.register("model")} type="text" className="field-base" />}
          />
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

function SelectField({ label, value, onChange, options, error, message, disabled }) {
  return (
    <div>
      <label className="field-label">{label}</label>
      <select value={value ?? ""} onChange={onChange} className="field-base" disabled={disabled}>
        <option value="">Selecciona una opcion</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <FieldError message={error ?? message} />
    </div>
  );
}
