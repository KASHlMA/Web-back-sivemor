import {
  alpha,
  Box,
  Card,
  CardContent,
  Grid,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { PagePanel, PageTitleBar, StatusChip } from "../../components/AdminPrimitives";
import { api } from "../../lib/api";
import { brandTokens } from "../../lib/brand";

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

      <PagePanel>
        <PageTitleBar
          title="Resumen de fallas"
          subtitle="Vista priorizada de unidades con problemas, fallas por región y reportes recientes."
        />
        <Grid container spacing={0}>
          <Grid item xs={12} lg={4} sx={{ borderRight: { lg: `1px solid ${brandTokens.colors.border}` } }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" gutterBottom>
                Fallas por región
              </Typography>
              <Stack spacing={1.25}>
                {(data?.failuresByRegion ?? []).map((item) => (
                  <Stack
                    key={item.label}
                    direction="row"
                    justifyContent="space-between"
                    sx={{
                      px: 1.5,
                      py: 1.1,
                      borderRadius: 1,
                      backgroundColor: alpha(brandTokens.colors.panelAlt, 0.8),
                      border: `1px solid ${brandTokens.colors.border}`
                    }}
                  >
                    <Typography>{item.label}</Typography>
                    <StatusChip label={String(item.count)} tone="warning" />
                  </Stack>
                ))}
              </Stack>
            </Box>
          </Grid>
          <Grid item xs={12} lg={8}>
            <Box sx={{ p: 3, overflowX: "auto" }}>
              <Typography variant="h5" gutterBottom>
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
                      <TableCell>
                        <StatusChip label={String(report.failureCount)} tone={report.failureCount > 0 ? "danger" : "success"} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Grid>
        </Grid>
      </PagePanel>
    </Stack>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <Card
      sx={{
        borderRadius: 1.5,
        border: `1px solid ${brandTokens.colors.border}`,
        boxShadow: brandTokens.shadow,
        backgroundColor: brandTokens.colors.panel
      }}
    >
      <CardContent>
        <Typography sx={{ color: brandTokens.colors.shellText }} gutterBottom>
          {label}
        </Typography>
        <Typography variant="h3" fontWeight={800} sx={{ color: brandTokens.colors.title }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}
