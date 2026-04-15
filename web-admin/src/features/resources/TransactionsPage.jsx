import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import {
  EmptyState,
  PagePanel,
  PageTitleBar,
  PrimaryActionButton,
  SecondaryActionButton
} from "../../components/AdminPrimitives";
import { renderStatusValue } from "../../components/ResourceTablePage";
import { api } from "../../lib/api";

export function TransactionDetailPage() {
  const navigate = useNavigate();
  const params = useParams({ strict: false });
  const transactionId = String(params.id ?? "");

  const transactionQuery = useQuery({
    queryKey: ["payments", transactionId],
    queryFn: () => api.get(`/admin/payments/${transactionId}`),
    enabled: Boolean(transactionId)
  });

  const details = useMemo(
    () => [
      { label: "Id de nota", value: transactionQuery.data?.orderNumber ?? "-" },
      { label: "Tipo de pago", value: transactionQuery.data?.paymentType ?? null, isStatus: true },
      { label: "Monto", value: formatCurrency(transactionQuery.data?.amount) },
      { label: "Cuenta de deposito", value: transactionQuery.data?.depositAccount ?? "-" },
      { label: "Numero de factura", value: transactionQuery.data?.invoiceNumber ?? "-" },
      { label: "Estatus", value: transactionQuery.data?.status ?? null, isStatus: true },
      { label: "Fecha de pago", value: formatDateTime(transactionQuery.data?.paidAt) }
    ],
    [transactionQuery.data]
  );

  if (transactionQuery.isLoading) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar title="Detalle de transaccion" />
          <div className="px-5 py-5 text-sm font-medium text-[var(--shell-text)]">Cargando informacion de la transaccion...</div>
        </PagePanel>
      </div>
    );
  }

  if (transactionQuery.isError || !transactionQuery.data) {
    return (
      <div className="space-y-6">
        <PagePanel>
          <PageTitleBar
            title="Detalle de transaccion"
            actions={
              <SecondaryActionButton type="button" onClick={() => void navigate({ to: "/transactions" })}>
                Volver
              </SecondaryActionButton>
            }
          />
          <EmptyState
            title="No se pudo cargar la informacion de la transaccion"
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
          title="Detalle de transaccion"
          subtitle="Consulta la informacion completa del pago registrado para la nota."
          actions={
            <div className="flex flex-col gap-3 sm:flex-row">
              <PrimaryActionButton type="button" onClick={() => void navigate({ to: "/transactions" })}>
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
      </PagePanel>
    </div>
  );
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
