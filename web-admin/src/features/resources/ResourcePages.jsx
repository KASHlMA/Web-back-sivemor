import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { z } from "zod";
import {
  ResourceTablePage,
  renderLinkedText,
  renderStatusValue,
  schemaHelpers
} from "../../components/ResourceTablePage";
import { SecondaryActionButton } from "../../components/AdminPrimitives";
import { api } from "../../lib/api";

const userSchema = z.object({
  username: schemaHelpers.requiredText("Usuario"),
  email: z.string().email("Correo inválido"),
  fullName: schemaHelpers.requiredText("Nombre completo"),
  role: z.enum(["ADMIN", "TECHNICIAN"]),
  active: z.boolean(),
  password: z.string().optional().or(z.literal(""))
});

const regionSchema = z.object({
  name: schemaHelpers.requiredText("Nombre")
});

const clientSchema = z.object({
  name: schemaHelpers.requiredText("Nombre"),
  taxId: schemaHelpers.requiredText("RFC"),
  regionId: z.string().min(1, "La región es obligatoria")
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
  orderNumber: schemaHelpers.requiredText("Número de pedido"),
  clientCompanyId: z.string().min(1, "El cliente es obligatorio"),
  regionId: z.string().min(1, "La región es obligatoria"),
  assignedTechnicianId: z.string().min(1, "El técnico es obligatorio"),
  unitIds: z.array(z.string()).min(1, "Selecciona al menos una unidad"),
  scheduledAt: schemaHelpers.requiredText("Fecha programada"),
  notes: z.string().optional().or(z.literal("")),
  status: z.enum(["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"])
});

const paymentSchema = z.object({
  verificationOrderId: z.string().min(1, "El pedido es obligatorio"),
  amount: z.string().min(1, "El monto es obligatorio"),
  currency: schemaHelpers.requiredText("Moneda"),
  status: z.enum(["PENDING", "PAID", "CANCELLED"]),
  reference: z.string().optional().or(z.literal("")),
  notes: z.string().optional().or(z.literal("")),
  paidAt: z.string().optional().or(z.literal(""))
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
  return (
    <ResourceTablePage
      title="Usuarios"
      description="Administra usuarios administrativos y técnicos de captura."
      endpoint="users"
      queryKey={["users"]}
      columns={[
        { header: "Usuario", render: (row) => renderLinkedText(row.username), searchableText: (row) => row.username },
        { header: "Nombre", render: (row) => row.fullName, searchableText: (row) => row.fullName },
        { header: "Correo", render: (row) => row.email, searchableText: (row) => row.email },
        { header: "Rol", render: (row) => renderStatusValue(row.role), searchableText: (row) => row.role },
        { header: "Activo", render: (row) => renderStatusValue(row.active), searchableText: (row) => String(row.active) }
      ]}
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
            { label: "Técnico", value: "TECHNICIAN" }
          ]
        },
        { name: "active", label: "Activo", type: "checkbox" },
        { name: "password", label: "Contraseña", type: "text" }
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
  const { regions } = useLookups();

  return (
    <ResourceTablePage
      title="Clientes"
      endpoint="clients"
      queryKey={["clients"]}
      columns={[
        { header: "Empresa", render: (row) => renderLinkedText(row.name), searchableText: (row) => row.name },
        { header: "RFC", render: (row) => row.taxId, searchableText: (row) => row.taxId },
        { header: "Región", render: (row) => row.regionName, searchableText: (row) => row.regionName }
      ]}
      schema={clientSchema}
      fields={[
        { name: "name", label: "Empresa", type: "text" },
        { name: "taxId", label: "RFC", type: "text" },
        {
          name: "regionId",
          label: "Región",
          type: "select",
          options: (regions.data ?? []).map((item) => ({
            label: item.name,
            value: String(item.id)
          }))
        }
      ]}
      defaultValues={{ name: "", taxId: "", regionId: "" }}
      toPayload={(values) => ({
        ...values,
        regionId: Number(values.regionId)
      })}
      toFormValues={(row) => ({
        name: row.name,
        taxId: row.taxId,
        regionId: String(row.regionId)
      })}
    />
  );
}

export function VehiclesPage() {
  const { clients } = useLookups();

  return (
    <ResourceTablePage
      title="Vehículos"
      endpoint="vehicles"
      queryKey={["vehicles"]}
      columns={[
        { header: "Placa", render: (row) => renderLinkedText(row.plate), searchableText: (row) => row.plate },
        { header: "Número de serie", render: (row) => row.vin, searchableText: (row) => row.vin },
        { header: "CEDIS", render: (row) => row.clientCompanyName, searchableText: (row) => row.clientCompanyName }
      ]}
      renderRowActions={() => (
        <div className="flex justify-end gap-2">
          <SecondaryActionButton type="button" disabled>
            Editar información
          </SecondaryActionButton>
          <SecondaryActionButton type="button" disabled>
            Ver historial
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
          label: "Categoría",
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
      title="Pedidos"
      endpoint="orders"
      queryKey={["orders"]}
      columns={[
        { header: "Pedido", render: (row) => renderLinkedText(row.orderNumber), searchableText: (row) => row.orderNumber },
        { header: "Empresa", render: (row) => row.clientCompanyName, searchableText: (row) => row.clientCompanyName },
        { header: "Región", render: (row) => row.regionName, searchableText: (row) => row.regionName },
        { header: "Técnico", render: (row) => row.assignedTechnicianName, searchableText: (row) => row.assignedTechnicianName },
        { header: "Estado", render: (row) => renderStatusValue(row.status), searchableText: (row) => row.status },
        {
          header: "No. de unidades",
          render: (row) => `Pedido de: ${row.units.length} unidades`,
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
  const { orders } = useLookups();

  return (
    <ResourceTablePage
      title="Pagos"
      endpoint="payments"
      queryKey={["payments"]}
      columns={[
        { header: "Pedido", render: (row) => renderLinkedText(row.orderNumber), searchableText: (row) => row.orderNumber },
        { header: "Monto", render: (row) => `${row.amount} ${row.currency}`, searchableText: (row) => String(row.amount) },
        { header: "Estado", render: (row) => renderStatusValue(row.status), searchableText: (row) => row.status },
        { header: "Referencia", render: (row) => row.reference ?? "-", searchableText: (row) => row.reference ?? "" }
      ]}
      schema={paymentSchema}
      fields={[
        {
          name: "verificationOrderId",
          label: "Pedido",
          type: "select",
          options: (orders.data ?? []).map((item) => ({
            label: item.orderNumber,
            value: String(item.id)
          }))
        },
        { name: "amount", label: "Monto", type: "number" },
        { name: "currency", label: "Moneda", type: "text" },
        {
          name: "status",
          label: "Estado",
          type: "select",
          options: [
            { label: "Pendiente", value: "PENDING" },
            { label: "Pagado", value: "PAID" },
            { label: "Cancelado", value: "CANCELLED" }
          ]
        },
        { name: "reference", label: "Referencia", type: "text" },
        { name: "notes", label: "Notas", type: "textarea" },
        { name: "paidAt", label: "Fecha de pago", type: "datetime" }
      ]}
      defaultValues={{
        verificationOrderId: "",
        amount: "",
        currency: "MXN",
        status: "PENDING",
        reference: "",
        notes: "",
        paidAt: ""
      }}
      toPayload={(values) => ({
        verificationOrderId: Number(values.verificationOrderId),
        amount: Number(values.amount),
        currency: values.currency,
        status: values.status,
        reference: values.reference || null,
        notes: values.notes || null,
        paidAt: values.paidAt ? new Date(values.paidAt).toISOString() : null
      })}
      toFormValues={(row) => ({
        verificationOrderId: String(row.verificationOrderId),
        amount: String(row.amount),
        currency: row.currency,
        status: row.status,
        reference: row.reference ?? "",
        notes: row.notes ?? "",
        paidAt: row.paidAt ? toDateTimeInputValue(row.paidAt) : ""
      })}
    />
  );
}

function toDateTimeInputValue(value) {
  return new Date(value).toISOString().slice(0, 16);
}
