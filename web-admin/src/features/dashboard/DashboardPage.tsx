import {
  Card,
  CardContent,
  Grid,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { api } from "../../lib/api";

type DashboardFailuresResponse = {
  totalSubmitted: number;
  totalFailed: number;
  unitsWithProblems: number;
  failuresByRegion: { label: string; count: number }[];
  recentFailures: {
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
  }[];
};

export function DashboardPage() {
  const query = useQuery({
    queryKey: ["dashboard-failures"],
    queryFn: () => api.get<DashboardFailuresResponse>("/admin/dashboard/failures")
  });

  const data = query.data;

  return (
    <Stack spacing={3}>
      <BoxHeader
        title="Resumen de fallas"
        description="Vista priorizada de unidades con problemas, fallas por región y reportes recientes."
      />

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <MetricCard label="Inspecciones enviadas" value={data?.totalSubmitted ?? 0} />
        </Grid>
        <Grid item xs={12} md={4}>
          <MetricCard label="Inspecciones con fallas" value={data?.totalFailed ?? 0} />
        </Grid>
        <Grid item xs={12} md={4}>
          <MetricCard label="Unidades con problemas" value={data?.unitsWithProblems ?? 0} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3, height: "100%" }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Fallas por región
            </Typography>
            <Stack spacing={1.5}>
              {(data?.failuresByRegion ?? []).map((item) => (
                <Stack
                  key={item.label}
                  direction="row"
                  justifyContent="space-between"
                  sx={{ p: 1.5, borderRadius: 2, backgroundColor: "rgba(15,23,42,0.04)" }}
                >
                  <Typography>{item.label}</Typography>
                  <Typography fontWeight={700}>{item.count}</Typography>
                </Stack>
              ))}
            </Stack>
          </Paper>
        </Grid>
        <Grid item xs={12} lg={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Reportes fallidos recientes
            </Typography>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Pedido</TableCell>
                  <TableCell>Empresa</TableCell>
                  <TableCell>Región</TableCell>
                  <TableCell>Técnico</TableCell>
                  <TableCell>Unidad</TableCell>
                  <TableCell>Fallas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(data?.recentFailures ?? []).map((report) => (
                  <TableRow key={report.inspectionId}>
                    <TableCell>{report.orderNumber}</TableCell>
                    <TableCell>{report.clientCompanyName}</TableCell>
                    <TableCell>{report.regionName}</TableCell>
                    <TableCell>{report.technicianName}</TableCell>
                    <TableCell>{report.vehiclePlate}</TableCell>
                    <TableCell>{report.failureCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>
      </Grid>
    </Stack>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <Card sx={{ borderRadius: 3 }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h3" fontWeight={800}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

function BoxHeader({ title, description }: { title: string; description: string }) {
  return (
    <Stack spacing={0.5}>
      <Typography variant="h4" fontWeight={700}>
        {title}
      </Typography>
      <Typography color="text.secondary">{description}</Typography>
    </Stack>
  );
}
