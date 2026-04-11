import { useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api } from "../lib/api";
import {
  AlertMessage,
  ConfirmDialog,
  EditIcon,
  EmptyState,
  FieldError,
  LinkText,
  Modal,
  PagePanel,
  PageTitleBar,
  PlusIcon,
  PrimaryActionButton,
  SearchField,
  SecondaryActionButton,
  StatusChip,
  TrashIcon
} from "./AdminPrimitives";

export function ResourceTablePage({
  title,
  description,
  endpoint,
  queryKey,
  columns,
  schema,
  fields,
  defaultValues,
  toPayload,
  toFormValues,
  renderRowActions
}) {
  const queryClient = useQueryClient();
  const [editingRow, setEditingRow] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [feedbackError, setFeedbackError] = useState(null);
  const [search, setSearch] = useState("");
  const [pendingDelete, setPendingDelete] = useState(null);

  const query = useQuery({
    queryKey,
    queryFn: () => api.get(`/admin/${endpoint}`)
  });

  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues
  });

  const createMutation = useMutation({
    mutationFn: (values) => api.post(`/admin/${endpoint}`, toPayload(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Request failed")
  });

  const updateMutation = useMutation({
    mutationFn: (values) => {
      if (!editingRow) {
        throw new Error("Missing row to update");
      }
      return api.put(`/admin/${endpoint}/${editingRow.id}`, toPayload(values));
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Request failed")
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/admin/${endpoint}/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      setPendingDelete(null);
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Delete failed")
  });

  const dialogTitle = useMemo(
    () => (editingRow ? `Editar ${title}` : `Nuevo ${title}`),
    [editingRow, title]
  );

  const filteredRows = useMemo(() => {
    const rows = query.data ?? [];
    const normalizedSearch = search.trim().toLowerCase();

    if (!normalizedSearch) {
      return rows;
    }

    return rows.filter((row) =>
      columns.some((column) => {
        const source = column.searchableText?.(row) ?? String(column.render(row));
        return source.toLowerCase().includes(normalizedSearch);
      })
    );
  }, [columns, query.data, search]);

  const closeDialog = () => {
    setDialogOpen(false);
    setEditingRow(null);
    setFeedbackError(null);
    form.reset(defaultValues);
  };

  const openCreateDialog = () => {
    setEditingRow(null);
    setFeedbackError(null);
    form.reset(defaultValues);
    setDialogOpen(true);
  };

  const openEditDialog = (row) => {
    setEditingRow(row);
    setFeedbackError(null);
    form.reset(toFormValues(row));
    setDialogOpen(true);
  };

  const onSubmit = form.handleSubmit(async (values) => {
    if (editingRow) {
      await updateMutation.mutateAsync(values);
      return;
    }
    await createMutation.mutateAsync(values);
  });

  const shouldRenderActions = renderRowActions !== null;

  return (
    <div className="space-y-6">
      <AlertMessage message={feedbackError} />

      <PagePanel>
        <PageTitleBar
          title={title}
          subtitle={description}
          search={<SearchField value={search} onChange={(event) => setSearch(event.target.value)} />}
          actions={
            <PrimaryActionButton type="button" onClick={openCreateDialog}>
              <PlusIcon />
              Agregar Nuevo
            </PrimaryActionButton>
          }
        />

        <div className="table-shell px-3 py-3 md:px-5">
          <table className="table-grid">
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column.header}>{column.header}</th>
                ))}
                {shouldRenderActions ? <th className="text-right">Acciones</th> : null}
              </tr>
            </thead>
            <tbody>
              {filteredRows.map((row) => (
                <tr key={row.id}>
                  {columns.map((column) => (
                    <td key={column.header}>{column.render(row)}</td>
                  ))}
                  {shouldRenderActions ? (
                    <td>
                      {renderRowActions ? (
                        renderRowActions(row)
                      ) : (
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => openEditDialog(row)}
                            className="inline-flex items-center gap-1 rounded-md px-2 py-1.5 text-sm font-semibold text-[var(--shell-text)] transition hover:bg-[var(--panel-alt)]"
                          >
                            <EditIcon />
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => setPendingDelete(row)}
                            className="inline-flex items-center gap-1 rounded-md px-2 py-1.5 text-sm font-semibold text-[var(--danger)] transition hover:bg-[#f9ebe7]"
                          >
                            <TrashIcon />
                            Eliminar
                          </button>
                        </div>
                      )}
                    </td>
                  ) : null}
                </tr>
              ))}
              {!query.isLoading && filteredRows.length === 0 ? (
                <tr>
                  <td colSpan={columns.length + (shouldRenderActions ? 1 : 0)} className="!p-0">
                    <EmptyState
                      title={search ? "No hay coincidencias" : "No hay registros disponibles"}
                      description={
                        search
                          ? "Prueba con otro término o limpia la búsqueda para volver a ver todos los registros."
                          : "Cuando existan elementos en este módulo aparecerán aquí."
                      }
                    />
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </PagePanel>

      {renderRowActions ? null : (
        <Modal
          open={dialogOpen}
          title={dialogTitle}
          onClose={closeDialog}
          footer={
            <>
              <SecondaryActionButton type="button" onClick={closeDialog}>
                Cancelar
              </SecondaryActionButton>
              <PrimaryActionButton
                type="button"
                onClick={() => void onSubmit()}
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                Guardar
              </PrimaryActionButton>
            </>
          }
        >
          <div className="space-y-4">
            {fields.map((field) => (
              <Controller
                key={field.name}
                name={field.name}
                control={form.control}
                render={({ field: controllerField, fieldState }) => (
                  <FieldRenderer
                    field={field}
                    controllerField={controllerField}
                    error={fieldState.error?.message}
                  />
                )}
              />
            ))}
          </div>
        </Modal>
      )}

      {renderRowActions ? null : (
        <ConfirmDialog
        open={Boolean(pendingDelete)}
        title={`Eliminar ${title}`}
        description={`¿Estás seguro de eliminar este registro de ${title.toLowerCase()}?`}
        confirmLabel="Eliminar"
        onCancel={() => setPendingDelete(null)}
        onConfirm={async () => {
          if (pendingDelete) {
            await deleteMutation.mutateAsync(pendingDelete.id);
          }
        }}
        danger
        />
      )}
    </div>
  );
}

function FieldRenderer({ field, controllerField, error }) {
  if (field.type === "checkbox") {
    return (
      <label className="flex items-center gap-3 rounded-lg border border-[var(--border)] bg-[#f8fbf4] px-4 py-3 text-sm font-semibold text-[var(--shell-text)]">
        <input
          type="checkbox"
          checked={Boolean(controllerField.value)}
          onChange={(event) => controllerField.onChange(event.target.checked)}
          className="h-4 w-4 rounded border-[var(--border-strong)] text-[var(--shell-dark)] focus:ring-[var(--shell-dark)]"
        />
        {field.label}
      </label>
    );
  }

  if (field.type === "select") {
    return (
      <div>
        <label className="field-label">{field.label}</label>
        <select
          value={controllerField.value ?? ""}
          onChange={(event) => controllerField.onChange(event.target.value)}
          className="field-base"
        >
          <option value="">Selecciona una opción</option>
          {(field.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <FieldError message={error} />
      </div>
    );
  }

  if (field.type === "multiselect") {
    return (
      <div>
        <label className="field-label">{field.label}</label>
        <select
          multiple
          value={controllerField.value ?? []}
          onChange={(event) => {
            const selectedValues = Array.from(event.target.selectedOptions).map((option) => option.value);
            controllerField.onChange(selectedValues);
          }}
          className="field-base min-h-[140px]"
        >
          {(field.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <p className="mt-1 text-xs text-[var(--shell-text)]/70">Mantén presionada la tecla Ctrl para seleccionar varias opciones.</p>
        <FieldError message={error} />
      </div>
    );
  }

  const inputType =
    field.type === "number" ? "number" : field.type === "datetime" ? "datetime-local" : "text";

  if (field.type === "textarea") {
    return (
      <div>
        <label className="field-label">{field.label}</label>
        <textarea
          value={controllerField.value ?? ""}
          onChange={controllerField.onChange}
          rows={4}
          className="field-base min-h-[112px] resize-y"
        />
        <FieldError message={error} />
      </div>
    );
  }

  return (
    <div>
      <label className="field-label">{field.label}</label>
      <input
        type={inputType}
        value={controllerField.value ?? ""}
        onChange={controllerField.onChange}
        className="field-base"
      />
      <FieldError message={error} />
    </div>
  );
}

export const schemaHelpers = {
  optionalText: z.string().optional().or(z.literal("")),
  requiredText: (label) => z.string().min(1, `${label} es obligatorio`)
};

export function renderStatusValue(value) {
  if (typeof value === "boolean") {
    return <StatusChip label={value ? "Activo" : "Inactivo"} tone={value ? "success" : "danger"} />;
  }

  if (!value) {
    return <span className="text-[var(--shell-text)]/60">-</span>;
  }

  const normalized = String(value).toUpperCase();
  const map = {
    ADMIN: { label: "Administrador", tone: "neutral" },
    TECHNICIAN: { label: "Técnico", tone: "neutral" },
    OPEN: { label: "Abierto", tone: "warning" },
    IN_PROGRESS: { label: "En progreso", tone: "warning" },
    COMPLETED: { label: "Completado", tone: "success" },
    CANCELLED: { label: "Cancelado", tone: "danger" },
    PENDING: { label: "Pendiente", tone: "warning" },
    PAID: { label: "Pagado", tone: "success" },
    N2: { label: "N2", tone: "neutral" },
    N3: { label: "N3", tone: "neutral" },
    PASS: { label: "Aprobado", tone: "success" },
    FAIL: { label: "Con fallas", tone: "danger" }
  };

  const resolved = map[normalized];
  return resolved ? <StatusChip label={resolved.label} tone={resolved.tone} /> : <span>{value}</span>;
}

export function renderLinkedText(value) {
  return <LinkText>{value}</LinkText>;
}
