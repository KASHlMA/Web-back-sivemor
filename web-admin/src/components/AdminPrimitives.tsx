import SearchRoundedIcon from "@mui/icons-material/SearchRounded";
import {
  alpha,
  Box,
  Button,
  Chip,
  InputAdornment,
  Paper,
  Stack,
  TextField,
  Typography,
  type ButtonProps,
  type PaperProps,
  type TextFieldProps
} from "@mui/material";
import type { PropsWithChildren, ReactNode } from "react";
import { brandTokens } from "../lib/brand";

export function PagePanel({ children, sx, ...props }: PropsWithChildren<PaperProps>) {
  return (
    <Paper
      {...props}
      elevation={0}
      sx={{
        borderRadius: brandTokens.radius.panel,
        border: `1px solid ${brandTokens.colors.border}`,
        boxShadow: brandTokens.shadow,
        overflow: "hidden",
        backgroundColor: brandTokens.colors.page,
        ...sx
      }}
    >
      {children}
    </Paper>
  );
}

export function PageTitleBar({
  title,
  subtitle,
  search,
  actions
}: {
  title: string;
  subtitle?: string;
  search?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <Stack
      direction={{ xs: "column", lg: "row" }}
      spacing={2}
      alignItems={{ xs: "stretch", lg: "center" }}
      justifyContent="space-between"
      sx={{
        px: { xs: 2, md: 3 },
        py: 2,
        backgroundColor: "#93b7a9",
        borderBottom: `1px solid ${brandTokens.colors.borderStrong}`
      }}
    >
      <Box>
        <Typography variant="h4" sx={{ color: brandTokens.colors.title }}>
          {title}
        </Typography>
        {subtitle ? (
          <Typography variant="body2" sx={{ mt: 0.5, color: alpha(brandTokens.colors.title, 0.82) }}>
            {subtitle}
          </Typography>
        ) : null}
      </Box>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems={{ sm: "center" }}>
        {search}
        {actions}
      </Stack>
    </Stack>
  );
}

export function SearchField(props: TextFieldProps) {
  return (
    <TextField
      size="small"
      placeholder="Buscar..."
      {...props}
      sx={{
        minWidth: { xs: "100%", sm: 220 },
        "& .MuiOutlinedInput-root": {
          backgroundColor: "#ffffff",
          borderRadius: brandTokens.radius.control,
          color: brandTokens.colors.shellText,
          "& fieldset": {
            borderColor: brandTokens.colors.borderStrong
          }
        },
        ...props.sx
      }}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchRoundedIcon sx={{ fontSize: 18, color: alpha(brandTokens.colors.shellText, 0.72) }} />
          </InputAdornment>
        ),
        ...props.InputProps
      }}
    />
  );
}

export function PrimaryActionButton(props: ButtonProps) {
  return (
    <Button
      variant="contained"
      {...props}
      sx={{
        minWidth: 156,
        borderRadius: brandTokens.radius.control,
        boxShadow: "none",
        textTransform: "none",
        fontWeight: 700,
        bgcolor: brandTokens.colors.shellDarkStrong,
        "&:hover": {
          bgcolor: "#0f3023",
          boxShadow: "none"
        },
        ...props.sx
      }}
    />
  );
}

export function StatusChip({
  label,
  tone = "neutral"
}: {
  label: string;
  tone?: "neutral" | "success" | "warning" | "danger";
}) {
  const colorMap = {
    neutral: { bg: "#edf2eb", fg: brandTokens.colors.shellText },
    success: { bg: "#dcecdf", fg: "#22543d" },
    warning: { bg: "#f5ead3", fg: "#8c5b11" },
    danger: { bg: "#f3ddd7", fg: brandTokens.colors.danger }
  } as const;

  return (
    <Chip
      label={label}
      size="small"
      sx={{
        borderRadius: 1,
        bgcolor: colorMap[tone].bg,
        color: colorMap[tone].fg,
        fontWeight: 700,
        height: 24
      }}
    />
  );
}

export function EmptyState({
  title,
  description
}: {
  title: string;
  description: string;
}) {
  return (
    <Stack spacing={1} alignItems="center" justifyContent="center" sx={{ minHeight: 220, px: 3, textAlign: "center" }}>
      <Typography variant="h6" sx={{ color: brandTokens.colors.title }}>
        {title}
      </Typography>
      <Typography variant="body2" sx={{ maxWidth: 420, color: alpha(brandTokens.colors.shellText, 0.86) }}>
        {description}
      </Typography>
    </Stack>
  );
}
