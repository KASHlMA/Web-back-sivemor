import AddIcon from "@mui/icons-material/Add";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Controller, useForm, type FieldValues, type Path } from "react-hook-form";
import { useMemo, useState, type ReactNode } from "react";
import { z, type ZodTypeAny } from "zod";
import { api } from "../lib/api";

export type SelectOption = {
  label: string;
  value: string | number;
};

export type ColumnDefinition<T> = {
  header: string;
  render: (row: T) => ReactNode;
};

export type FieldDefinition<F extends FieldValues = FieldValues> = {
  name: Path<F>;
  label: string;
  type: "text" | "textarea" | "number" | "checkbox" | "select" | "multiselect" | "datetime";
  options?: SelectOption[];
};

type ResourceTablePageProps<T extends { id: number }> = {
  title: string;
  description?: string;
  endpoint: string;
  queryKey: string[];
  columns: ColumnDefinition<T>[];
  schema: ZodTypeAny;
  fields: FieldDefinition[];
  defaultValues: Record<string, any>;
  toPayload: (values: Record<string, any>) => unknown;
  toFormValues: (row: T) => Record<string, any>;
};

export function ResourceTablePage<T extends { id: number }>({
  title,
  description,
  endpoint,
  queryKey,
  columns,
  schema,
  fields,
  defaultValues,
  toPayload,
  toFormValues
}: ResourceTablePageProps<T>) {
  const queryClient = useQueryClient();
  const [editingRow, setEditingRow] = useState<T | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [feedbackError, setFeedbackError] = useState<string | null>(null);

  const query = useQuery({
    queryKey,
    queryFn: () => api.get<T[]>(`/admin/${endpoint}`)
  });

  const form = useForm<Record<string, any>>({
    resolver: zodResolver(schema),
    defaultValues
  });

  const createMutation = useMutation({
    mutationFn: (values: Record<string, any>) => api.post<T>(`/admin/${endpoint}`, toPayload(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Request failed")
  });

  const updateMutation = useMutation({
    mutationFn: (values: Record<string, any>) => {
      if (!editingRow) {
        throw new Error("Missing row to update");
      }

      return api.put<T>(`/admin/${endpoint}/${editingRow.id}`, toPayload(values));
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
      closeDialog();
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Request failed")
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/admin/${endpoint}/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey });
    },
    onError: (error) => setFeedbackError(error instanceof Error ? error.message : "Delete failed")
  });

  const dialogTitle = useMemo(
    () => (editingRow ? `Editar ${title}` : `Nuevo ${title}`),
    [editingRow, title]
  );

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

  const openEditDialog = (row: T) => {
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

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={2}>
        <Box>
          <Typography variant="h4" fontWeight={700}>
            {title}
          </Typography>
          {description ? (
            <Typography color="text.secondary">{description}</Typography>
          ) : null}
        </Box>
        <Button startIcon={<AddIcon />} variant="contained" onClick={openCreateDialog}>
          Nuevo
        </Button>
      </Box>

      {feedbackError ? <Alert severity="error">{feedbackError}</Alert> : null}

      <Paper sx={{ overflowX: "auto" }}>
        <Table>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell key={column.header}>{column.header}</TableCell>
              ))}
              <TableCell align="right">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(query.data ?? []).map((row) => (
              <TableRow key={row.id} hover>
                {columns.map((column) => (
                  <TableCell key={column.header}>{column.render(row)}</TableCell>
                ))}
                <TableCell align="right">
                  <Button size="small" startIcon={<EditOutlinedIcon />} onClick={() => openEditDialog(row)}>
                    Editar
                  </Button>
                  <Button
                    size="small"
                    color="error"
                    startIcon={<DeleteOutlineIcon />}
                    onClick={() => {
                      if (window.confirm(`¿Archivar registro de ${title.toLowerCase()}?`)) {
                        void deleteMutation.mutateAsync(row.id);
                      }
                    }}
                  >
                    Eliminar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {!query.isLoading && (query.data?.length ?? 0) === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1}>
                  <Typography color="text.secondary">
                    No hay registros disponibles.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : null}
          </TableBody>
        </Table>
      </Paper>

      <Dialog open={dialogOpen} onClose={closeDialog} fullWidth maxWidth="md">
        <DialogTitle>{dialogTitle}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {fields.map((field) => {
              if (field.type === "checkbox") {
                return (
                  <Controller
                    key={field.name}
                    name={field.name}
                    control={form.control}
                    render={({ field: controllerField }) => (
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={Boolean(controllerField.value)}
                            onChange={(_, checked) => controllerField.onChange(checked)}
                          />
                        }
                        label={field.label}
                      />
                    )}
                  />
                );
              }

              if (field.type === "select" || field.type === "multiselect") {
                return (
                  <Controller
                    key={field.name}
                    name={field.name}
                    control={form.control}
                    render={({ field: controllerField, fieldState }) => (
                      <TextField
                        select
                        fullWidth
                        SelectProps={{ multiple: field.type === "multiselect" }}
                        label={field.label}
                        value={controllerField.value ?? (field.type === "multiselect" ? [] : "")}
                        onChange={controllerField.onChange}
                        error={Boolean(fieldState.error)}
                        helperText={fieldState.error?.message}
                      >
                        {(field.options ?? []).map((option) => (
                          <MenuItem key={option.value} value={option.value}>
                            {option.label}
                          </MenuItem>
                        ))}
                      </TextField>
                    )}
                  />
                );
              }

              return (
                <Controller
                  key={field.name}
                  name={field.name}
                  control={form.control}
                  render={({ field: controllerField, fieldState }) => (
                    <TextField
                      {...controllerField}
                      fullWidth
                      label={field.label}
                      type={field.type === "number" ? "number" : field.type === "datetime" ? "datetime-local" : "text"}
                      multiline={field.type === "textarea"}
                      minRows={field.type === "textarea" ? 3 : undefined}
                      value={controllerField.value ?? ""}
                      onChange={controllerField.onChange}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message}
                      InputLabelProps={field.type === "datetime" ? { shrink: true } : undefined}
                    />
                  )}
                />
              );
            })}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancelar</Button>
          <Button
            variant="contained"
            onClick={() => void onSubmit()}
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            Guardar
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

export const schemaHelpers = {
  optionalText: z.string().optional().or(z.literal("")),
  requiredText: (label: string) => z.string().min(1, `${label} es obligatorio`)
};
