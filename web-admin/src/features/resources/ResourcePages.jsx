import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import {
  ResourceTablePage,
  renderLinkedText,
  renderStatusValue,
  schemaHelpers
} from "../../components/ResourceTablePage";
import { SecondaryActionButton } from "../../components/AdminPrimitives";
import { api } from "../../lib/api";
import { consumeFlashMessage } from "../../lib/flashMessage";

const userSchema = z.object({
  username: schemaHelpers.requiredText("Usuario"),
  email: schemaHelpers.email("Correo"),
  fullName: schemaHelpers.requiredText("Nombre completo"),
  role: z.enum(["ADMIN", "TECHNICIAN"]),
  active: z.boolean()
});

const regionSchema = z.object({
  name: schemaHelpers.requiredText("Nombre")
});

const vehicleSchema = z.object({
  clientCompanyId: z.string().min(1, "El cliente es obligatorio"),
  plate: schemaHelpers.requiredText("Placa"),
  vin: schemaHelpers.requiredText("VIN"),
  category: z.enum(["N2", "N3"]),
  brand: schemaHelpers.requiredText("Marca"),
  model: schemaHelpers.requiredText("Modelo")
});

const orderSchema = z.object({
  orderNumber: schemaHelpers.requiredText("Número de nota"),
  clientCompanyId: z.string().min(1, "El cliente es obligatorio"),
  regionId: z.string().min(1, "La región es obligatoria"),
  assignedTechnicianId: z.string().min(1, "El técnico es obligatorio"),
  unitIds: z.array(z.string()).min(1, "Selecciona al menos una unidad"),
  scheduledAt: schemaHelpers.requiredText("Fecha programada"),
  notes: schemaHelpers.optionalText,
  status: z.enum(["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"])
});

const paymentSchema = z.object({
  verificationOrderId: z.string().min(1, "La nota es obligatoria"),
  paymentType: z.enum(["CASH", "CARD"]),
  amount: z.string().min(1, "El monto es obligatorio"),
  status: z.enum(["PENDING", "APPROVED"]),
  depositAccount: schemaHelpers.optionalText,
  invoiceNumber: schemaHelpers.optionalText,
  paidAt: z.string().optional().or(z.literal(""))
});

const physicalDocumentOrderSchema = z.object({
  verificationOrderId: z.string().min(1, "La nota es obligatoria"),
  shippedAt: schemaHelpers.requiredText("Fecha de env?o"),
  trackingNumber: z.string().trim().max(120, "La gu?a no puede exceder 120 caracteres").optional().or(z.literal("")),
  status: z.enum(["ORDERED", "SHIPPED", "DELIVERED", "CANCELLED"]),
  receivedBy: z.string().trim().max(160, "Qui?n recibi? no puede exceder 160 caracteres").optional().or(z.literal("")),
  photoData: z.string().optional().or(z.literal("")),
  comment: z.string().trim().max(3000, "El comentario es demasiado largo").optional().or(z.literal(""))
});

function useLookups() {
  const users = useQuery({
    queryKey: ["users-lookup"],
    queryFn: () => api.get("/admin/users")
  });
  const regions = useQuery({
    queryKey: ["regions-lookup"],
    queryFn: () => api.get("/admin/regions")
  });
  const clients = useQuery({
    queryKey: ["clients-lookup"],
    queryFn: () => api.get("/admin/clients")
  });
  const vehicles = useQuery({
    queryKey: ["vehicles-lookup"],
    queryFn: () => api.get("/admin/vehicles")
  });
  const orders = useQuery({
    queryKey: ["orders-lookup"],
    queryFn: () => api.get("/admin/orders")
  });

  return { users, regions, clients, vehicles, orders };
}

export function UsersPage() {
  const queryClient = useQueryClient();
  const [feedbackMessage, setFeedbackMessage] = useState(null);
  const resetPasswordMutation = useMutation({
    mutationFn: (userId) => api.post(`/admin/users/${userId}/password/reset`, {}),
    onSuccess: async (response) => {
      setFeedbackMessage(response?.message ?? "La nueva contraseÃƒÂ±a fue enviada por correo");
      await queryClient.invalidateQueries({ queryKey: ["users"] });
      await queryClient.invalidateQueries({ queryKey: ["users-lookup"] });
    },
    onError: (error) => {
      setFeedbackMessage(error instanceof Error ? error.message : "No se pudo regenerar la contraseÃƒÂ±a");
    }
  });

  return (
    <ResourceTablePage
      title="Usuarios"
      description="Administra usuarios administrativos y tÃ©cnicos de captura."
      endpoint="users"
      queryKey={["users"]}
      feedbackMessage={feedbackMessage}
      createLabel="Agregar nuevo usuario"
      emptyDescription="Cuando registres usuarios, su contraseÃƒÆ’Ã‚Â±a se generarÃƒÆ’Ã‚Â¡ automÃƒÆ’Ã‚Â¡ticamente y se enviarÃƒÆ’Ã‚Â¡ a su correo."
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
              void resetPasswordMutation.mutateAsync(row.id);
            }}
            disabled={resetPasswordMutation.isPending}
          >
            Generar contraseÃƒÆ’Ã‚Â±a
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
            { label: "TÃ©cnico", value: "TECHNICIAN" }
          ]
        },
        { name: "active", label: "Activo", type: "checkbox" },
        { name: "password", label: "ContraseÃ±a", type: "text" }
      ]}
      defaultValues={{
        username: "",
        email: "",
        fullName: "",
        role: "TECHNICIAN",
        active: true,
        password: ""
      }}
      toPayload={(values) => values}
      toFormValues={(row) => ({
        username: row.username,
        email: row.email,
        fullName: row.fullName,
        role: row.role,
        active: row.active,
        password: ""
      })}
    />
  );
}

export function RegionsPage() {
  return (
    <ResourceTablePage
      title="Regiones"
      endpoint="regions"
      queryKey={["regions"]}
      columns={[{ header: "Nombre", render: (row) => renderLinkedText(row.name), searchableText: (row) => row.name }]}
      schema={regionSchema}
      fields={[{ name: "name", label: "Nombre", type: "text" }]}
      defaultValues={{ name: "" }}
      toPayload={(values) => values}
      toFormValues={(row) => ({ name: row.name })}
    />
  );
}

export function ClientsPage() {
  const navigate = useNavigate();
  const [feedbackMessage] = useState(() => consumeFlashMessage());

  return (
    <ResourceTablePage
      title="Clientes"
      description="Administra los clientes registrados en el sistema."
      endpoint="clients"
      queryKey={["clients"]}
      feedbackMessage={feedbackMessage}
      createLabel="Agregar nuevo cliente"
      loadingMessage="Cargando clientes..."
      emptyTitle="No hay clientes registrados"
      emptyDescription="Cuando existan clientes registrados aparecer?n aqu?."
      errorMessage="Error al cargar los clientes"
      deleteDialogTitle="Eliminar cliente"
      deleteDialogDescription="Deseas eliminar este cliente?"
      deleteSuccessMessage="Cliente eliminado correctamente"
      extraInvalidateQueryKeys={[["clients-lookup"]]}
      onCreateAction={() => void navigate({ to: "/clients/nuevo" })}
      columns={[
        { header: "Nombre", render: (row) => renderLinkedText(row.name), searchableText: (row) => row.name },
        { header: "Correo", render: (row) => row.email, searchableText: (row) => row.email },
        { header: "Teléfono", render: (row) => row.phone, searchableText: (row) => row.phone },
        {
          header: "Teléfono alternativo",
          render: (row) => row.alternatePhone,
          searchableText: (row) => row.alternatePhone
        }
      ]}
      renderRowActions={(row, { requestDelete }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/clients/$id", params: { id: String(row.id) } })}
          >
            Ver información
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={z.object({
        name: schemaHelpers.requiredText("Nombre"),
        businessName: schemaHelpers.requiredText("Razón social"),
        email: schemaHelpers.email("Correo"),
        phone: schemaHelpers.phone("Teléfono"),
        alternatePhone: schemaHelpers.phone("Teléfono alternativo"),
        manager: schemaHelpers.requiredText("Gestor")
      })}
      fields={[
        { name: "name", label: "Nombre", type: "text" },
        { name: "businessName", label: "Razón social", type: "text" },
        { name: "email", label: "Correo", type: "text" },
        { name: "phone", label: "Teléfono", type: "text" },
        { name: "alternatePhone", label: "Teléfono alternativo", type: "text" },
        { name: "manager", label: "Gestor", type: "text" }
      ]}
      defaultValues={{
        name: "",
        businessName: "",
        email: "",
        phone: "",
        alternatePhone: "",
        manager: ""
      }}
      toPayload={(values) => values}
      toFormValues={(row) => ({
        name: row.name,
        businessName: row.businessName,
        email: row.email,
        phone: row.phone,
        alternatePhone: row.alternatePhone,
        manager: row.manager
      })}
    />
  );
}

export function VehiclesPage() {
  const navigate = useNavigate();
  const { clients } = useLookups();
  const [feedbackMessage] = useState(() => consumeFlashMessage());

  return (
    <ResourceTablePage
      title="Vehiculos"
      endpoint="vehicles"
      queryKey={["vehicles"]}
      feedbackMessage={feedbackMessage}
      deleteDialogTitle="Eliminar vehiculo"
      deleteDialogDescription="Deseas eliminar este vehiculo?"
      deleteSuccessMessage="Vehiculo eliminado correctamente"
      extraInvalidateQueryKeys={[["vehicles-lookup"]]}
      onCreateAction={() => void navigate({ to: "/vehiculos/nuevo" })}
      columns={[
        { header: "Placa", render: (row) => renderLinkedText(row.plate), searchableText: (row) => row.plate },
        { header: "Numero de serie", render: (row) => row.vin, searchableText: (row) => row.vin },
        { header: "CEDIS", render: (row) => row.clientCompanyName, searchableText: (row) => row.clientCompanyName }
      ]}
      renderRowActions={(row, { requestDelete }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/vehiculos/$id", params: { id: String(row.id) } })}
          >
            Ver informacion
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={vehicleSchema}
      fields={[
        {
          name: "clientCompanyId",
          label: "Cliente",
          type: "select",
          options: (clients.data ?? []).map((item) => ({
            label: item.name,
            value: String(item.id)
          }))
        },
        { name: "plate", label: "Placa", type: "text" },
        { name: "vin", label: "VIN", type: "text" },
        {
          name: "category",
          label: "CategorÃ­a",
          type: "select",
          options: [
            { label: "N2", value: "N2" },
            { label: "N3", value: "N3" }
          ]
        },
        { name: "brand", label: "Marca", type: "text" },
        { name: "model", label: "Modelo", type: "text" }
      ]}
      defaultValues={{
        clientCompanyId: "",
        plate: "",
        vin: "",
        category: "N2",
        brand: "",
        model: ""
      }}
      toPayload={(values) => ({
        ...values,
        clientCompanyId: Number(values.clientCompanyId)
      })}
      toFormValues={(row) => ({
        clientCompanyId: String(row.clientCompanyId),
        plate: row.plate,
        vin: row.vin,
        category: row.category,
        brand: row.brand,
        model: row.model
      })}
    />
  );
}

export function CedisPage() {
  const navigate = useNavigate();
  const [feedbackMessage] = useState(() => consumeFlashMessage());

  return (
    <ResourceTablePage
      title="CEDIS"
      description="Administra los centros de distribucion registrados en el sistema."
      endpoint="cedis"
      queryKey={["cedis"]}
      feedbackMessage={feedbackMessage}
      createLabel="Agregar nuevo CEDIS"
      loadingMessage="Cargando CEDIS..."
      emptyTitle="No hay CEDIS registrados"
      emptyDescription="Cuando existan CEDIS registrados apareceran aqui."
      errorMessage="Error al cargar los CEDIS"
      deleteDialogTitle="Eliminar CEDIS"
      deleteDialogDescription="Deseas eliminar este CEDIS?"
      deleteSuccessMessage="CEDIS eliminado correctamente"
      onCreateAction={() => void navigate({ to: "/cedis/nuevo" })}
      columns={[
        { header: "Nombre", render: (row) => renderLinkedText(row.name), searchableText: (row) => row.name },
        { header: "Correo", render: (row) => row.email, searchableText: (row) => row.email },
        { header: "Telefono", render: (row) => row.phone, searchableText: (row) => row.phone },
        {
          header: "Telefono alternativo",
          render: (row) => row.alternatePhone,
          searchableText: (row) => row.alternatePhone
        }
      ]}
      renderRowActions={(row, { requestDelete }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/cedis/$id", params: { id: String(row.id) } })}
          >
            Ver informacion
          </SecondaryActionButton>
          <SecondaryActionButton
            type="button"
            onClick={() => requestDelete(row)}
          >
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={z.object({
        name: schemaHelpers.requiredText("Nombre"),
        email: schemaHelpers.email("Correo"),
        phone: schemaHelpers.phone("Telefono"),
        alternatePhone: schemaHelpers.phone("Telefono alternativo")
      })}
      fields={[
        { name: "name", label: "Nombre", type: "text" },
        { name: "email", label: "Correo", type: "text" },
        { name: "phone", label: "Telefono", type: "text" },
        { name: "alternatePhone", label: "Telefono alternativo", type: "text" }
      ]}
      defaultValues={{
        name: "",
        email: "",
        phone: "",
        alternatePhone: ""
      }}
      toPayload={(values) => values}
      toFormValues={(row) => ({
        name: row.name,
        email: row.email,
        phone: row.phone,
        alternatePhone: row.alternatePhone
      })}
    />
  );
}

export function VerificationCentersPage() {
  const navigate = useNavigate();
  const [feedbackMessage] = useState(() => consumeFlashMessage());

  return (
    <ResourceTablePage
      title="Verificentros"
      description="Administra los verificentros registrados en el sistema."
      endpoint="verification-centers"
      queryKey={["verification-centers"]}
      feedbackMessage={feedbackMessage}
      createLabel="Agregar nuevo verificentro"
      loadingMessage="Cargando verificentros..."
      emptyTitle="No hay verificentros registrados"
      emptyDescription="Cuando existan verificentros registrados aparecer?n aqu?."
      errorMessage="Error al cargar los verificentros"
      deleteDialogTitle="Eliminar verificentro"
      deleteDialogDescription="Deseas eliminar este verificentro?"
      deleteSuccessMessage="Verificentro eliminado correctamente"
      onCreateAction={() => void navigate({ to: "/verification-centers/nuevo" })}
      columns={[
        { header: "Nombre", render: (row) => renderLinkedText(row.name), searchableText: (row) => row.name },
        { header: "Correo", render: (row) => row.email, searchableText: (row) => row.email },
        { header: "Teléfono", render: (row) => row.phone, searchableText: (row) => row.phone },
        {
          header: "Teléfono alternativo",
          render: (row) => row.alternatePhone,
          searchableText: (row) => row.alternatePhone
        }
      ]}
      renderRowActions={(row, { requestDelete }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/verification-centers/$id", params: { id: String(row.id) } })}
          >
            Ver información
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={z.object({
        name: schemaHelpers.requiredText("Nombre"),
        centerKey: schemaHelpers.requiredText("Clave de verificentro"),
        address: schemaHelpers.requiredText("Dirección"),
        regionId: z.string().min(1, "Región es obligatoria"),
        manager: schemaHelpers.requiredText("Responsable"),
        email: schemaHelpers.email("Correo"),
        phone: schemaHelpers.phone("Teléfono"),
        alternatePhone: schemaHelpers.phone("Teléfono alternativo"),
        schedule: schemaHelpers.requiredText("Horario")
      })}
      fields={[
        { name: "name", label: "Nombre", type: "text" },
        { name: "centerKey", label: "Clave de verificentro", type: "text" },
        { name: "address", label: "Dirección", type: "text" },
        { name: "regionId", label: "Región", type: "text" },
        { name: "manager", label: "Responsable", type: "text" },
        { name: "email", label: "Correo", type: "text" },
        { name: "phone", label: "Teléfono", type: "text" },
        { name: "alternatePhone", label: "Teléfono alternativo", type: "text" },
        { name: "schedule", label: "Horario", type: "text" }
      ]}
      defaultValues={{
        name: "",
        centerKey: "",
        address: "",
        regionId: "",
        manager: "",
        email: "",
        phone: "",
        alternatePhone: "",
        schedule: ""
      }}
      toPayload={(values) => values}
      toFormValues={(row) => row}
    />
  );
}

export function OrdersPage() {
  const { users, regions, clients, vehicles } = useLookups();

  const technicianOptions = useMemo(
    () =>
      (users.data ?? [])
        .filter((item) => item.role === "TECHNICIAN")
        .map((item) => ({ label: item.fullName, value: String(item.id) })),
    [users.data]
  );

  return (
    <ResourceTablePage
      title="Notas"
      endpoint="orders"
      queryKey={["orders"]}
      bulkActions={[
        {
          key: "complete",
          label: "Marcar como completadas",
          confirmTitle: "Completar notas seleccionadas",
          confirmDescription: (rows) =>
            `Se marcaran como completadas ${rows.length} nota${rows.length === 1 ? "" : "s"} seleccionada${rows.length === 1 ? "" : "s"}.`,
          confirmLabel: "Completar",
          successMessage: "Notas actualizadas correctamente",
          action: (row) =>
            api.put(`/admin/orders/${row.id}`, {
              orderNumber: row.orderNumber,
              clientCompanyId: Number(row.clientCompanyId),
              regionId: Number(row.regionId),
              assignedTechnicianId: Number(row.assignedTechnicianId),
              unitIds: [],
              scheduledAt: row.scheduledAt,
              notes: row.notes ?? null,
              status: "COMPLETED"
            })
        },
        {
          key: "delete",
          label: "Eliminar seleccionadas",
          confirmTitle: "Eliminar notas seleccionadas",
          confirmDescription: (rows) =>
            `Se eliminaran ${rows.length} nota${rows.length === 1 ? "" : "s"} seleccionada${rows.length === 1 ? "" : "s"}. Esta accion no se puede deshacer.`,
          confirmLabel: "Eliminar",
          successMessage: "Notas eliminadas correctamente",
          danger: true,
          action: (row) => api.delete(`/admin/orders/${row.id}`)
        }
      ]}
      columns={[
        { header: "Nota", render: (row) => renderLinkedText(row.orderNumber), searchableText: (row) => row.orderNumber },
        { header: "Empresa", render: (row) => row.clientCompanyName, searchableText: (row) => row.clientCompanyName },
        { header: "Región", render: (row) => row.regionName, searchableText: (row) => row.regionName },
        { header: "Técnico", render: (row) => row.assignedTechnicianName, searchableText: (row) => row.assignedTechnicianName },
        { header: "Estado", render: (row) => renderStatusValue(row.status), searchableText: (row) => row.status },
        {
          header: "No. de unidades",
          render: (row) => `Nota de: ${row.units.length} unidades`,
          searchableText: (row) => row.units.map((item) => item.vehiclePlate).join(" ")
        }
      ]}
      schema={orderSchema}
      fields={[
        { name: "orderNumber", label: "Número de pedido", type: "text" },
        {
          name: "clientCompanyId",
          label: "Cliente",
          type: "select",
          options: (clients.data ?? []).map((item) => ({
            label: item.name,
            value: String(item.id)
          }))
        },
        {
          name: "regionId",
          label: "Región",
          type: "select",
          options: (regions.data ?? []).map((item) => ({
            label: item.name,
            value: String(item.id)
          }))
        },
        {
          name: "assignedTechnicianId",
          label: "Técnico asignado",
          type: "select",
          options: technicianOptions
        },
        {
          name: "unitIds",
          label: "Unidades",
          type: "multiselect",
          options: (vehicles.data ?? []).map((item) => ({
            label: `${item.plate} (${item.category})`,
            value: String(item.id)
          }))
        },
        { name: "scheduledAt", label: "Fecha programada", type: "datetime" },
        { name: "notes", label: "Notas", type: "textarea" },
        {
          name: "status",
          label: "Estado",
          type: "select",
          options: [
            { label: "Abierto", value: "OPEN" },
            { label: "En progreso", value: "IN_PROGRESS" },
            { label: "Completado", value: "COMPLETED" },
            { label: "Cancelado", value: "CANCELLED" }
          ]
        }
      ]}
      defaultValues={{
        orderNumber: "",
        clientCompanyId: "",
        regionId: "",
        assignedTechnicianId: "",
        unitIds: [],
        scheduledAt: "",
        notes: "",
        status: "OPEN"
      }}
      toPayload={(values) => ({
        ...values,
        clientCompanyId: Number(values.clientCompanyId),
        regionId: Number(values.regionId),
        assignedTechnicianId: Number(values.assignedTechnicianId),
        unitIds: values.unitIds.map((item) => Number(item)),
        scheduledAt: new Date(values.scheduledAt).toISOString()
      })}
      toFormValues={(row) => ({
        orderNumber: row.orderNumber,
        clientCompanyId: String(row.clientCompanyId),
        regionId: String(row.regionId),
        assignedTechnicianId: String(row.assignedTechnicianId),
        unitIds: row.units.map((item) => String(item.vehicleUnitId)),
        scheduledAt: toDateTimeInputValue(row.scheduledAt),
        notes: row.notes ?? "",
        status: row.status
      })}
    />
  );
}

export function PaymentsPage() {
  const navigate = useNavigate();
  const { orders } = useLookups();

  return (
    <ResourceTablePage
      title="Transacciones"
      description="Registra los pagos realizados para cada nota y consulta su informacion completa."
      endpoint="payments"
      queryKey={["payments"]}
      createLabel="Agregar nueva transaccion"
      loadingMessage="Cargando transacciones..."
      emptyTitle="No hay transacciones registradas"
      emptyDescription="Cuando registres pagos relacionados a una nota apareceran aqui."
      errorMessage="Error al cargar las transacciones"
      deleteDialogTitle="Eliminar transaccion"
      deleteDialogDescription="Deseas eliminar esta transaccion?"
      deleteSuccessMessage="Transaccion eliminada correctamente"
      bulkActions={[
        {
          key: "approve",
          label: "Marcar como aprobadas",
          confirmTitle: "Aprobar transacciones seleccionadas",
          confirmDescription: (rows) =>
            `Se marcaran como aprobadas ${rows.length} transaccion${rows.length === 1 ? "" : "es"} seleccionada${rows.length === 1 ? "" : "s"}.`,
          confirmLabel: "Aprobar",
          successMessage: "Transacciones actualizadas correctamente",
          action: (row) =>
            api.put(`/admin/payments/${row.id}`, {
              verificationOrderId: Number(row.verificationOrderId),
              paymentType: row.paymentType,
              amount: Number(row.amount),
              status: "APPROVED",
              depositAccount: row.depositAccount ?? null,
              invoiceNumber: row.invoiceNumber ?? null,
              paidAt: row.paidAt ?? null
            })
        },
        {
          key: "delete",
          label: "Eliminar seleccionadas",
          confirmTitle: "Eliminar transacciones seleccionadas",
          confirmDescription: (rows) =>
            `Se eliminaran ${rows.length} transaccion${rows.length === 1 ? "" : "es"} seleccionada${rows.length === 1 ? "" : "s"}. Esta accion no se puede deshacer.`,
          confirmLabel: "Eliminar",
          successMessage: "Transacciones eliminadas correctamente",
          danger: true,
          action: (row) => api.delete(`/admin/payments/${row.id}`)
        }
      ]}
      columns={[
        {
          header: "Id nota",
          render: (row) => renderLinkedText(row.orderNumber),
          searchableText: (row) => `${row.orderNumber} ${row.verificationOrderId}`
        },
        {
          header: "Metodo de pago",
          render: (row) => renderStatusValue(row.paymentType),
          searchableText: (row) => row.paymentType
        },
        { header: "Monto", render: (row) => formatCurrency(row.amount), searchableText: (row) => String(row.amount) },
        { header: "Estado", render: (row) => renderStatusValue(row.status), searchableText: (row) => row.status },
        { header: "Fecha", render: (row) => formatDateTime(row.paidAt), searchableText: (row) => row.paidAt ?? "" }
      ]}
      renderRowActions={(row, { requestDelete, openEditDialog }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/transactions/$id", params: { id: String(row.id) } })}
          >
            Ver informacion
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => openEditDialog(row)}>
            Editar
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={paymentSchema}
      fields={[
        {
          name: "verificationOrderId",
          label: "Nota",
          type: "select",
          options: (orders.data ?? []).map((item) => ({
            label: item.orderNumber,
            value: String(item.id)
          }))
        },
        {
          name: "paymentType",
          label: "Tipo de pago",
          type: "select",
          options: [
            { label: "Efectivo", value: "CASH" },
            { label: "Tarjeta", value: "CARD" }
          ]
        },
        { name: "amount", label: "Monto", type: "number" },
        {
          name: "status",
          label: "Estatus",
          type: "select",
          options: [
            { label: "Pendiente", value: "PENDING" },
            { label: "Aprobado", value: "APPROVED" }
          ]
        },
        { name: "depositAccount", label: "Cuenta de deposito", type: "text" },
        { name: "invoiceNumber", label: "Numero de factura", type: "text" },
        { name: "paidAt", label: "Fecha de pago", type: "datetime" }
      ]}
      defaultValues={{
        verificationOrderId: "",
        paymentType: "CASH",
        amount: "",
        status: "PENDING",
        depositAccount: "",
        invoiceNumber: "",
        paidAt: ""
      }}
      toPayload={(values) => ({
        verificationOrderId: Number(values.verificationOrderId),
        paymentType: values.paymentType,
        amount: Number(values.amount),
        status: values.status,
        depositAccount: values.depositAccount || null,
        invoiceNumber: values.invoiceNumber || null,
        paidAt: values.paidAt ? new Date(values.paidAt).toISOString() : null
      })}
      toFormValues={(row) => ({
        verificationOrderId: String(row.verificationOrderId),
        paymentType: row.paymentType,
        amount: String(row.amount),
        status: row.status,
        depositAccount: row.depositAccount ?? "",
        invoiceNumber: row.invoiceNumber ?? "",
        paidAt: row.paidAt ? toDateTimeInputValue(row.paidAt) : ""
      })}
    />
  );
}

export function PhysicalDocumentOrdersPage() {
  const navigate = useNavigate();
  const { orders } = useLookups();

  return (
    <ResourceTablePage
      title="Pedidos"
      description="Registra cuando un cliente solicita el documento fisico relacionado con una nota."
      endpoint="physical-document-orders"
      queryKey={["physical-document-orders"]}
      createLabel="Agregar nuevo pedido"
      loadingMessage="Cargando pedidos..."
      emptyTitle="No hay pedidos registrados"
      emptyDescription="Cuando registres pedidos de documentos fisicos apareceran aqui."
      errorMessage="Error al cargar los pedidos"
      deleteDialogTitle="Eliminar pedido"
      deleteDialogDescription="Deseas eliminar este pedido?"
      deleteSuccessMessage="Pedido eliminado correctamente"
      bulkActions={[
        {
          key: "deliver",
          label: "Marcar como entregados",
          confirmTitle: "Completar pedidos seleccionados",
          confirmDescription: (rows) =>
            `Se marcaran como entregados ${rows.length} pedido${rows.length === 1 ? "" : "s"} seleccionada${rows.length === 1 ? "" : "s"}.`,
          confirmLabel: "Entregar",
          successMessage: "Pedidos actualizados correctamente",
          action: (row) =>
            api.put(`/admin/physical-document-orders/${row.id}`, {
              verificationOrderId: Number(row.verificationOrderId),
              shippedAt: row.shippedAt,
              trackingNumber: row.trackingNumber ?? null,
              status: "DELIVERED",
              receivedBy: row.receivedBy ?? null,
              photoData: row.photoData ?? null,
              comment: row.comment ?? null
            })
        },
        {
          key: "delete",
          label: "Eliminar seleccionados",
          confirmTitle: "Eliminar pedidos seleccionados",
          confirmDescription: (rows) =>
            `Se eliminaran ${rows.length} pedido${rows.length === 1 ? "" : "s"} seleccionada${rows.length === 1 ? "" : "s"}. Esta accion no se puede deshacer.`,
          confirmLabel: "Eliminar",
          successMessage: "Pedidos eliminados correctamente",
          danger: true,
          action: (row) => api.delete(`/admin/physical-document-orders/${row.id}`)
        }
      ]}
      columns={[
        { header: "Folio de nota", render: (row) => renderLinkedText(row.noteNumber), searchableText: (row) => row.noteNumber },
        { header: "Fecha de envio", render: (row) => formatDateTime(row.shippedAt), searchableText: (row) => row.shippedAt },
        { header: "Numero de guia", render: (row) => row.trackingNumber ?? "-", searchableText: (row) => row.trackingNumber ?? "" },
        { header: "Estatus", render: (row) => renderStatusValue(row.status), searchableText: (row) => row.status }
      ]}
      renderRowActions={(row, { requestDelete, openEditDialog }) => (
        <div className="table-actions">
          <SecondaryActionButton
            type="button"
            onClick={() => void navigate({ to: "/pedidos/$id", params: { id: String(row.id) } })}
          >
            Ver informacion
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => openEditDialog(row)}>
            Editar
          </SecondaryActionButton>
          <SecondaryActionButton type="button" onClick={() => requestDelete(row)}>
            Eliminar
          </SecondaryActionButton>
        </div>
      )}
      schema={physicalDocumentOrderSchema}
      fields={[
        {
          name: "verificationOrderId",
          label: "Folio de nota",
          type: "select",
          options: (orders.data ?? []).map((item) => ({
            label: item.orderNumber,
            value: String(item.id)
          }))
        },
        { name: "shippedAt", label: "Fecha de envio", type: "datetime" },
        { name: "trackingNumber", label: "Numero de guia", type: "text" },
        {
          name: "status",
          label: "Estatus",
          type: "select",
          options: [
            { label: "Pedido", value: "ORDERED" },
            { label: "Enviado", value: "SHIPPED" },
            { label: "Entregado", value: "DELIVERED" },
            { label: "Cancelado", value: "CANCELLED" }
          ]
        },
        { name: "receivedBy", label: "Quien recibio", type: "text" },
        { name: "photoData", label: "Foto", type: "image" },
        { name: "comment", label: "Comentario", type: "textarea" }
      ]}
      defaultValues={{
        verificationOrderId: "",
        shippedAt: "",
        trackingNumber: "",
        status: "ORDERED",
        receivedBy: "",
        photoData: "",
        comment: ""
      }}
      toPayload={(values) => ({
        verificationOrderId: Number(values.verificationOrderId),
        shippedAt: new Date(values.shippedAt).toISOString(),
        trackingNumber: values.trackingNumber || null,
        status: values.status,
        receivedBy: values.receivedBy || null,
        photoData: values.photoData || null,
        comment: values.comment || null
      })}
      toFormValues={(row) => ({
        verificationOrderId: String(row.verificationOrderId),
        shippedAt: toDateTimeInputValue(row.shippedAt),
        trackingNumber: row.trackingNumber ?? "",
        status: row.status,
        receivedBy: row.receivedBy ?? "",
        photoData: row.photoData ?? "",
        comment: row.comment ?? ""
      })}
    />
  );
}

function toDateTimeInputValue(value) {
  return new Date(value).toISOString().slice(0, 16);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("es-MX", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

function formatCurrency(value) {
  return new Intl.NumberFormat("es-MX", {
    style: "currency",
    currency: "MXN"
  }).format(Number(value ?? 0));
}

