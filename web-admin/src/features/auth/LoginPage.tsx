import {
  Alert,
  Box,
  Button,
  Stack,
  TextField,
  Typography
} from "@mui/material";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { Controller, useForm } from "react-hook-form";
import { useEffect } from "react";
import { z } from "zod";
import { brandAssets, brandTokens } from "../../lib/brand";
import { useAuth } from "../../lib/session";

const schema = z.object({
  username: z.string().min(1, "El usuario es obligatorio"),
  password: z.string().min(1, "La contraseña es obligatoria")
});

type LoginFormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const { session, login } = useAuth();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      username: "admin",
      password: "Admin123!"
    }
  });

  const mutation = useMutation({
    mutationFn: async (values: LoginFormValues) => {
      await login(values.username, values.password);
    },
    onSuccess: () => {
      void navigate({ to: "/" });
    }
  });

  useEffect(() => {
    if (session) {
      void navigate({ to: "/" });
    }
  }, [navigate, session]);

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "grid",
        gridTemplateColumns: { xs: "1fr", md: "0.58fr 1fr" },
        backgroundColor: brandTokens.colors.page
      }}
    >
      <Box
        sx={{
          display: { xs: "none", md: "block" },
          position: "relative",
          overflow: "hidden",
          minHeight: "100vh",
          backgroundColor: "#557665"
        }}
      >
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            clipPath: "polygon(18% 0, 100% 0, 60% 100%, 0 100%)",
            backgroundColor: brandTokens.colors.headerAccent
          }}
        />
      </Box>

      <Stack
        justifyContent="center"
        alignItems="center"
        sx={{ px: { xs: 3, md: 6 }, py: 6 }}
      >
        <Stack spacing={3.5} sx={{ width: "100%", maxWidth: 408, alignItems: "center" }}>
          <Box
            component="img"
            src={brandAssets.logoDark}
            alt="SIVEMOR"
            sx={{ width: 234, maxWidth: "78%" }}
          />
          <Box sx={{ textAlign: "center" }}>
            <Typography variant="h4" sx={{ color: brandTokens.colors.title }}>
              SIVEMOR
            </Typography>
            <Typography variant="body2" sx={{ color: brandTokens.colors.title }}>
              Sistema de Verificación de Morelos
            </Typography>
          </Box>

          <Typography variant="h4" sx={{ fontSize: "2rem", color: brandTokens.colors.title }}>
            Iniciar sesión
          </Typography>

          {mutation.error ? (
            <Alert severity="error" sx={{ width: "100%" }}>
              {mutation.error instanceof Error ? mutation.error.message : "No fue posible iniciar sesión"}
            </Alert>
          ) : null}

          <Controller
            name="username"
            control={form.control}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                fullWidth
                placeholder="Email"
                error={Boolean(fieldState.error)}
                helperText={fieldState.error?.message}
                sx={{
                  "& .MuiOutlinedInput-root": {
                    backgroundColor: "#fff",
                    minHeight: 51,
                    "& fieldset": {
                      borderColor: brandTokens.colors.borderStrong
                    }
                  }
                }}
              />
            )}
          />

          <Controller
            name="password"
            control={form.control}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                fullWidth
                placeholder="Password"
                type="password"
                error={Boolean(fieldState.error)}
                helperText={fieldState.error?.message}
                sx={{
                  "& .MuiOutlinedInput-root": {
                    backgroundColor: "#fff",
                    minHeight: 51,
                    "& fieldset": {
                      borderColor: brandTokens.colors.borderStrong
                    }
                  }
                }}
              />
            )}
          />

          <Button
            variant="contained"
            onClick={form.handleSubmit(async (values) => {
              await mutation.mutateAsync(values);
            })}
            disabled={mutation.isPending}
            sx={{
              width: 158,
              minHeight: 37,
              bgcolor: "#97aaa0",
              color: "#fff",
              boxShadow: "none",
              "&:hover": {
                bgcolor: "#7f9589",
                boxShadow: "none"
              }
            }}
          >
            Iniciar Sesión
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
