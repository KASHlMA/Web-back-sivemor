import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import {
  ResourceTablePage,
  renderLinkedText,
  renderStatusValue,
  schemaHelpers
} from "../../components/ResourceTablePage";
import { ConfirmDialog, SecondaryActionButton } from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

const userSchema = z.object({
  username: schemaHelpers.requiredText("Usuario"),
  email: schemaHelpers.email("Correo"),
  fullName: schemaHelpers.requiredText("Nombre completo"),
  role: z.enum(["ADMIN", "TECHNICIAN"]),
  active: z.boolean()
});

export function UsersPage() {
  const queryClient = useQueryClient();
  const [feedbackMessage, setFeedbackMessage] = useState(null);
  const [pendingResetUser, setPendingResetUser] = useState(null);
  const resetPasswordMutation = useMutation({
    mutationFn: (userId) => api.post(`/admin/users/${userId}/password/reset`, {}),
    onSuccess: async (response) => {
      setFeedbackMessage(response?.message ?? "La nueva contrasena fue enviada por correo");
      setPendingResetUser(null);
      await queryClient.invalidateQueries({ queryKey: ["users"] });
      await queryClient.invalidateQueries({ queryKey: ["users-lookup"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No se pudo regenerar la contrasena");
    }
  });

  return (
    <>
      <ResourceTablePage
      title="Usuarios"
      description="Administra usuarios administrativos y tecnicos de captura."
      endpoint="users"
      queryKey={["users"]}
      feedbackMessage={feedbackMessage}
      createLabel="Agregar nuevo usuario"
      emptyDescription="Cuando registres usuarios, su contrasena se generara automaticamente y se enviara a su correo."
      columns={[
        { header: "Usuario", render: (row) => renderLinkedText(row.username), searchableText: (row) => row.username },
        { header: "Nombre", render: (row) => row.fullName, searchableText: (row) => row.fullName },
        { header: "Correo", render: (row) => row.email, searchableText: (row) => row.email },
        { header: "Rol", render: (row) => renderStatusValue(row.role), searchableText: (row) => row.role },
        { header: "Activo", render: (row) => renderStatusValue(row.active), searchableText: (row) => String(row.active) }
      ]}
      renderRowActions={(row, { requestDelete, openEditDialog }) => (
        <div className="table-actions">
          <SecondaryActionButton type="button" onClick={() => openEditDialog(row)}>
            Editar
          </SecondaryActionButton>
          <SecondaryActionButton
            type="button"
            onClick={() => {
              setFeedbackMessage(null);
              setPendingResetUser(row);
            }}
            disabled={resetPasswordMutation.isPending}
          >
            Generar contrasena
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={userSchema}
      fields={[
        { name: "username", label: "Usuario", type: "text" },
        { name: "email", label: "Correo", type: "text" },
        { name: "fullName", label: "Nombre completo", type: "text" },
        {
          name: "role",
          label: "Rol",
          type: "select",
          options: [
            { label: "Administrador", value: "ADMIN" },
            { label: "Tecnico", value: "TECHNICIAN" }
          ]
        },
        { name: "active", label: "Activo", type: "checkbox" }
      ]}
      defaultValues={{
        username: "",
        email: "",
        fullName: "",
        role: "TECHNICIAN",
        active: true
      }}
      toPayload={(values) => values}
      toFormValues={(row) => ({
        username: row.username,
        email: row.email,
        fullName: row.fullName,
        role: row.role,
        active: row.active
      })}
      />
      <ConfirmDialog
        open={Boolean(pendingResetUser)}
        title="Confirmar cambio de contrasena"
        description={
          pendingResetUser
            ? `Se generara una nueva contrasena para ${pendingResetUser.fullName}. Deseas continuar?`
            : ""
        }
        confirmLabel="Cambiar contrasena"
        cancelLabel="Cancelar"
        onCancel={() => setPendingResetUser(null)}
        onConfirm={async () => {
          if (pendingResetUser) {
            setFeedbackMessage(null);
            await resetPasswordMutation.mutateAsync(pendingResetUser.id);
          }
        }}
      />
    </>
  );
}
