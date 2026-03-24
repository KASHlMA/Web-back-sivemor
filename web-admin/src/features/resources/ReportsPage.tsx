import {
  Box,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField
} from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { PagePanel, PageTitleBar, PrimaryActionButton } from "../../components/AdminPrimitives";
import { renderStatusValue } from "../../components/ResourceTablePage";
import { api } from "../../lib/api";

type Report = {
  inspectionId: number;
  orderNumber: string;
  clientCompanyName: string;
  regionName: string;
  technicianName: string;
  vehiclePlate: string;
  vehicleCategory: string;
  submittedAt: string | null;
  overallResult: string | null;
  failureCount: number;
  evidenceCount: number;
};

type Lookup = { id: number; name?: string; fullName?: string; orderNumber?: string; plate?: string };

function useLookup<T extends Lookup>(key: string, path: string) {
  return useQuery({
    queryKey: [key],
    queryFn: () => api.get<T[]>(path)
  });
}

export function ReportsPage() {
  const [filters, setFilters] = useState({
    companyId: "",
    regionId: "",
    orderId: "",
    technicianId: "",
    vehicleId: "",
    onlyFailures: "true"
  });

  const clients = useLookup<{ id: number; name: string }>("clients-lookup", "/admin/clients");
  const regions = useLookup<{ id: number; name: string }>("regions-lookup", "/admin/regions");
  const users = useLookup<{ id: number; fullName: string; role: string }>("users-lookup", "/admin/users");
  const vehicles = useLookup<{ id: number; plate: string }>("vehicles-lookup", "/admin/vehicles");
  const orders = useLookup<{ id: number; orderNumber: string }>("orders-lookup", "/admin/orders");

  const queryString = useMemo(() => {
    const params = new URLSearchParams();
    if (filters.companyId) params.set("companyId", filters.companyId);
    if (filters.regionId) params.set("regionId", filters.regionId);
    if (filters.orderId) params.set("orderId", filters.orderId);
    if (filters.technicianId) params.set("technicianId", filters.technicianId);
    if (filters.vehicleId) params.set("vehicleId", filters.vehicleId);
    params.set("onlyFailures", filters.onlyFailures);
    return params.toString();
  }, [filters]);

  const reports = useQuery({
    queryKey: ["reports", queryString],
    queryFn: () => api.get<Report[]>(`/admin/reports?${queryString}`)
  });

  return (
    <Stack spacing={3}>
      <PagePanel>
        <PageTitleBar
          title="Reportes de fallas"
          subtitle="Filtra por empresa, región, pedido, técnico y unidad para concentrarte en las fallas detectadas."
          actions={
            <PrimaryActionButton
              variant="outlined"
              sx={{
                bgcolor: "transparent",
                color: "white",
                border: "none",
                minWidth: 120,
                "&:hover": {
                  bgcolor: "rgba(19,64,46,0.14)"
                }
              }}
              onClick={() =>
                setFilters({
                  companyId: "",
                  regionId: "",
                  orderId: "",
                  technicianId: "",
                  vehicleId: "",
                  onlyFailures: "true"
                })
              }
            >
              Limpiar
            </PrimaryActionButton>
          }
        />

        <Stack direction={{ xs: "column", md: "row" }} spacing={2} flexWrap="wrap" sx={{ p: 3 }}>
          <FilterSelect
            label="Empresa"
            value={filters.companyId}
            onChange={(value) => setFilters((current) => ({ ...current, companyId: value }))}
            options={(clients.data ?? []).map((item) => ({ label: item.name, value: String(item.id) }))}
          />
          <FilterSelect
            label="Región"
            value={filters.regionId}
            onChange={(value) => setFilters((current) => ({ ...current, regionId: value }))}
            options={(regions.data ?? []).map((item) => ({ label: item.name, value: String(item.id) }))}
          />
          <FilterSelect
            label="Pedido"
            value={filters.orderId}
            onChange={(value) => setFilters((current) => ({ ...current, orderId: value }))}
            options={(orders.data ?? []).map((item) => ({ label: item.orderNumber, value: String(item.id) }))}
          />
          <FilterSelect
            label="Técnico"
            value={filters.technicianId}
            onChange={(value) => setFilters((current) => ({ ...current, technicianId: value }))}
            options={(users.data ?? [])
              .filter((item) => item.role === "TECHNICIAN")
              .map((item) => ({ label: item.fullName, value: String(item.id) }))}
          />
          <FilterSelect
            label="Unidad"
            value={filters.vehicleId}
            onChange={(value) => setFilters((current) => ({ ...current, vehicleId: value }))}
            options={(vehicles.data ?? []).map((item) => ({ label: item.plate, value: String(item.id) }))}
          />
          <FilterSelect
            label="Visibilidad"
            value={filters.onlyFailures}
            onChange={(value) => setFilters((current) => ({ ...current, onlyFailures: value }))}
            options={[
              { label: "Solo fallas", value: "true" },
              { label: "Todos", value: "false" }
            ]}
          />
        </Stack>

        <Box sx={{ overflowX: "auto", px: 2.5, pb: 2.5 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Pedido</TableCell>
                <TableCell>Empresa</TableCell>
                <TableCell>Región</TableCell>
                <TableCell>Técnico</TableCell>
                <TableCell>Unidad</TableCell>
                <TableCell>Resultado</TableCell>
                <TableCell>Fallas</TableCell>
                <TableCell>Evidencias</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(reports.data ?? []).map((report) => (
                <TableRow key={report.inspectionId}>
                  <TableCell>{report.orderNumber}</TableCell>
                  <TableCell>{report.clientCompanyName}</TableCell>
                  <TableCell>{report.regionName}</TableCell>
                  <TableCell>{report.technicianName}</TableCell>
                  <TableCell>{report.vehiclePlate}</TableCell>
                  <TableCell>{renderStatusValue(report.overallResult ?? "PENDING")}</TableCell>
                  <TableCell>{renderStatusValue(report.failureCount > 0 ? "FAIL" : "PASS")}</TableCell>
                  <TableCell>{report.evidenceCount}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      </PagePanel>
    </Stack>
  );
}

function FilterSelect({
  label,
  value,
  onChange,
  options
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
}) {
  return (
    <TextField
      select
      label={label}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      sx={{ minWidth: 180 }}
    >
      <MenuItem value="">Todos</MenuItem>
      {options.map((option) => (
        <MenuItem key={option.value} value={option.value}>
          {option.label}
        </MenuItem>
      ))}
    </TextField>
  );
}
