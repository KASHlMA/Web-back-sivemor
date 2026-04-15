import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import {
  EmptyState,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton,
  StatusChip
} from "../../components/AdminPrimitives";

import { renderStatusValue } from "../../components/ResourceTablePage";
import { api } from "../../lib/api";

export function PhysicalDocumentOrderDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const orderId = String(params.id ?? "");

  const orderQuery = useQuery({
    queryKey: ["physical-document-orders", orderId],
    queryFn: () => api.get(`/admin/physical-document-orders/${orderId}`),
    enabled: Boolean(orderId)
  });

  const details = useMemo(
    () => [
      { label: "Folio de nota", value: orderQuery.data?.noteNumber ?? "-" },
      { label: "Fecha de envio", value: orderQuery.data ? formatDateTime(orderQuery.data.shippedAt) : "-" },
      { label: "Numero de guia", value: orderQuery.data?.trackingNumber ?? "-" },
      { label: "Estatus", value: orderQuery.data?.status ?? null, isStatus: true },
      { label: "Quien recibio", value: orderQuery.data?.receivedBy ?? "-" },
      { label: "Comentario", value: orderQuery.data?.comment ?? "-" }
    ],
    [orderQuery.data]
  );

  if (orderQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Detalle de pedido" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando informacion del pedido...</div>
        </PagePanel>
      </div>
    );
  }

  if (orderQuery.isError || !orderQuery.data) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Detalle de pedido"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/pedidos" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="No se pudo cargar la informacion del pedido"
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
          title="Detalle de pedido"
          subtitle="Consulta la informacion completa del envio del documento fisico."
          actions={
            <div className="action-group">
              <PrimaryActionButton type="button" onClick={() => void navigate({ to: "/pedidos" })}>
                Volver al listado
              </PrimaryActionButton>
            </div>
          }
        />

        <div className="grid gap-4 px-5 py-5 md:grid-cols-2">
          {details.map((item) => (
            <div key={item.label} className="rounded-xl border border-[var(--border)] bg-[#f8fbf4] px-4 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.04em] text-[var(--shell-text)]/75">{item.label}</p>
              <div className="mt-2 text-base font-semibold text-[var(--title)]">
                {item.isStatus ? renderStatusValue(item.value) : item.value}
              </div>
            </div>
          ))}
        </div>

        <div className="px-5 pb-5">
          <div className="rounded-xl border border-[var(--border)] bg-[#f8fbf4] p-4">
            <div className="flex items-center justify-between gap-3">
              <p className="text-xs font-semibold uppercase tracking-[0.04em] text-[var(--shell-text)]/75">Foto</p>
              <StatusChip label={orderQuery.data.photoData ? "Adjunta" : "Sin foto"} tone={orderQuery.data.photoData ? "success" : "warning"} />
            </div>
            {orderQuery.data.photoData ? (
              <img
                src={orderQuery.data.photoData}
                alt={`Pedido ${orderQuery.data.noteNumber}`}
                className="mt-4 max-h-[420px] w-full rounded-xl border border-[var(--border)] object-contain bg-white"
              />
            ) : (
              <p className="mt-3 text-sm text-[var(--shell-text)]">Este pedido no tiene imagen registrada.</p>
            )}
          </div>
        </div>
      </PagePanel>
    </div>
  );
}

function formatDateTime(value) {
  return new Intl.DateTimeFormat("es-MX", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}
