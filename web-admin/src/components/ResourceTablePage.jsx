import { useEffect, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api } from "../lib/api";
import {
  ActionGroup,
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
  renderRowActions,
  onCreateAction,
  feedbackMessage,
  createLabel = "Agregar nuevo",
  loadingMessage = "Cargando registros...",
  emptyTitle = "No hay registros disponibles",
  emptyDescription = "Cuando existan elementos en este modulo apareceran aqui.",
  errorMessage = "Error al cargar los registros",
  deleteDialogTitle,
  deleteDialogDescription,
  deleteSuccessMessage,
  extraInvalidateQueryKeys = [],
  pageSize = 10
}) {
  const queryClient = useQueryClient();
  const [editingRow, setEditingRow] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [feedbackError, setFeedbackError] = useState(null);
  const [localFeedbackMessage, setLocalFeedbackMessage] = useState(null);
  const [search, setSearch] = useState("");
  const [pendingDelete, setPendingDelete] = useState(null);
  const [page, setPage] = useState(1);

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
      await Promise.all(extraInvalidateQueryKeys.map((key) => queryClient.invalidateQueries({ queryKey: key })));
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "No se pudo crear el registro")
  });

  const updateMutation = useMutation({
    mutationFn: (values) => {
      if (!editingRow) {
        throw new Error("Falta el registro a actualizar");
      }
      return api.put(`/admin/${endpoint}/${editingRow.id}`, toPayload(values));
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      await Promise.all(extraInvalidateQueryKeys.map((key) => queryClient.invalidateQueries({ queryKey: key })));
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "No se pudo actualizar el registro")
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/admin/${endpoint}/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      await Promise.all(extraInvalidateQueryKeys.map((key) => queryClient.invalidateQueries({ queryKey: key })));
      setPendingDelete(null);
      setLocalFeedbackMessage(deleteSuccessMessage ?? null);
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "No se pudo eliminar el registro")
  });

  const dialogTitle = useMemo(() => (editingRow ? `Editar ${title}` : `Nuevo ${title}`), [editingRow, title]);

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

  const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));

  const paginatedRows = useMemo(() => {
    const startIndex = (page - 1) * pageSize;
    return filteredRows.slice(startIndex, startIndex + pageSize);
  }, [filteredRows, page, pageSize]);

  useEffect(() => {
    setPage(1);
  }, [search]);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

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
  const resolvedDeleteDialogTitle = deleteDialogTitle ?? `Eliminar ${title}`;
  const resolvedDeleteDialogDescription =
    deleteDialogDescription ?? `Estas seguro de eliminar este registro de ${title.toLowerCase()}?`;
  const hasRows = filteredRows.length > 0;

  return (
    <div className="space-y-6">
      <AlertMessage message={feedbackError} />
      <AlertMessage message={feedbackMessage} />
      <AlertMessage message={localFeedbackMessage} />

      <PagePanel>
        <PageTitleBar
          title={title}
          subtitle={description}
          search={<SearchField value={search} onChange={(event) => setSearch(event.target.value)} />}
          actions={
            <PrimaryActionButton type="button" onClick={onCreateAction ?? openCreateDialog}>
              <PlusIcon />
              {createLabel}
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
              {query.isLoading ? (
                <tr>
                  <td colSpan={columns.length + (shouldRenderActions ? 1 : 0)} className="px-4 py-6 text-sm font-medium text-[var(--shell-text)]">
                    {loadingMessage}
                  </td>
                </tr>
              ) : null}
              {!query.isLoading && query.isError ? (
                <tr>
                  <td colSpan={columns.length + (shouldRenderActions ? 1 : 0)} className="!p-0">
                    <EmptyState title={errorMessage} description="Intenta nuevamente en unos momentos." />
                  </td>
                </tr>
              ) : null}
              {!query.isLoading && !query.isError
                ? paginatedRows.map((row) => (
                    <tr key={row.id}>
                      {columns.map((column) => (
                        <td key={column.header}>{column.render(row)}</td>
                      ))}
                      {shouldRenderActions ? (
                        <td>
                          {renderRowActions ? (
                            renderRowActions(row, { requestDelete: setPendingDelete, openEditDialog })
                          ) : (
                            <ActionGroup className="table-actions">
                              <SecondaryActionButton type="button" onClick={() => openEditDialog(row)}>
                                <EditIcon />
                                Editar
                              </SecondaryActionButton>
                              <SecondaryActionButton type="button" onClick={() => setPendingDelete(row)}>
                                <TrashIcon />
                                Eliminar
                              </SecondaryActionButton>
                            </ActionGroup>
                          )}
                        </td>
                      ) : null}
                    </tr>
                  ))
                : null}
              {!query.isLoading && !query.isError && filteredRows.length === 0 ? (
                <tr>
                  <td colSpan={columns.length + (shouldRenderActions ? 1 : 0)} className="!p-0">
                    <EmptyState
                      title={search ? "No hay coincidencias" : emptyTitle}
                      description={
                        search
                          ? "Prueba con otro termino o limpia la busqueda para volver a ver todos los registros."
                          : emptyDescription
                      }
                    />
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>

          {!query.isLoading && !query.isError && hasRows ? (
            <div className="mt-4 flex flex-col gap-3 border-t border-[var(--border)] px-2 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <span className="text-sm font-medium text-[var(--shell-text)]">Pagina {page} de {totalPages}</span>
              <div className="flex gap-2">
                <SecondaryActionButton type="button" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={page === 1}>
                  Anterior
                </SecondaryActionButton>
                <SecondaryActionButton type="button" onClick={() => setPage((current) => Math.min(totalPages, current + 1))} disabled={page === totalPages}>
                  Siguiente
                </SecondaryActionButton>
              </div>
            </div>
          ) : null}
        </div>
      </PagePanel>

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
                <FieldRenderer field={field} controllerField={controllerField} error={fieldState.error?.message} />
              )}
            />
          ))}
        </div>
      </Modal>

      {renderRowActions ? (
        pendingDelete ? (
          <ConfirmDialog
            open={Boolean(pendingDelete)}
            title={resolvedDeleteDialogTitle}
            description={resolvedDeleteDialogDescription}
            confirmLabel="Eliminar"
            onCancel={() => setPendingDelete(null)}
            onConfirm={async () => {
              await deleteMutation.mutateAsync(pendingDelete.id);
            }}
            danger
          />
        ) : null
      ) : (
        <ConfirmDialog
          open={Boolean(pendingDelete)}
          title={resolvedDeleteDialogTitle}
          description={resolvedDeleteDialogDescription}
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
          <option value="">Selecciona una opcion</option>
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
        <p className="mt-1 text-xs text-[var(--shell-text)]/70">Manten presionada la tecla Ctrl para seleccionar varias opciones.</p>
        <FieldError message={error} />
      </div>
    );
  }

  const inputType = field.type === "number" ? "number" : field.type === "datetime" ? "datetime-local" : "text";

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

  if (field.type === "image") {
    const preview = controllerField.value || "";

    return (
      <div>
        <label className="field-label">{field.label}</label>
        <div className="space-y-3 rounded-lg border border-[var(--border)] bg-[#f8fbf4] p-4">
          <input
            type="file"
            accept="image/*"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (!file) {
                controllerField.onChange("");
                return;
              }

              const reader = new FileReader();
              reader.onload = () => controllerField.onChange(typeof reader.result === "string" ? reader.result : "");
              reader.readAsDataURL(file);
            }}
            className="block w-full text-sm text-[var(--shell-text)] file:mr-4 file:rounded-md file:border-0 file:bg-[var(--shell-dark)] file:px-3 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-[var(--shell-dark-strong)]"
          />
          {preview ? (
            <div className="space-y-2">
              <img src={preview} alt={field.label} className="max-h-48 rounded-lg border border-[var(--border)] object-contain" />
              <SecondaryActionButton type="button" onClick={() => controllerField.onChange("")}>
                Quitar foto
              </SecondaryActionButton>
            </div>
          ) : (
            <p className="text-xs text-[var(--shell-text)]/75">Selecciona una imagen para guardarla junto con el pedido.</p>
          )}
        </div>
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
  optionalText: z
    .string()
    .trim()
    .refine((value) => value === "" || value.length >= 2, {
      message: "Debe contener al menos 2 caracteres"
    })
    .optional()
    .or(z.literal("")),
  requiredText: (label) =>
    z
      .string()
      .trim()
      .min(1, `${label} es obligatorio`)
      .min(2, `${label} debe contener al menos 2 caracteres`),
  email: (label = "Correo") =>
    z
      .string()
      .trim()
      .min(1, `${label} es obligatorio`)
      .email(`${label} invalido`),
  phone: (label = "Telefono") =>
    z
      .string()
      .trim()
      .min(1, `${label} es obligatorio`)
      .refine((value) => /^\d{9,}$/.test(value), {
        message: `${label} debe contener al menos 9 digitos`
      })
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
    TECHNICIAN: { label: "Tecnico", tone: "neutral" },
    OPEN: { label: "Abierto", tone: "warning" },
    IN_PROGRESS: { label: "En progreso", tone: "warning" },
    COMPLETED: { label: "Completado", tone: "success" },
    CANCELLED: { label: "Cancelado", tone: "danger" },
    PENDING: { label: "Pendiente", tone: "warning" },
    APPROVED: { label: "Pagado", tone: "success" },
    PAID: { label: "Pagado", tone: "success" },
    CASH: { label: "Efectivo", tone: "neutral" },
    CARD: { label: "Tarjeta", tone: "neutral" },
    ORDERED: { label: "Pedido", tone: "warning" },
    SHIPPED: { label: "Enviado", tone: "neutral" },
    DELIVERED: { label: "Entregado", tone: "success" },
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
