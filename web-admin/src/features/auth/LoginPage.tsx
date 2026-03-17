import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
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
        placeItems: "center",
        background:
          "radial-gradient(circle at top left, rgba(18,91,141,0.2), transparent 40%), #0f172a",
        p: 3
      }}
    >
      <Card sx={{ width: "100%", maxWidth: 460, borderRadius: 4 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h4" fontWeight={700}>
                SIVEMOR Admin
              </Typography>
              <Typography color="text.secondary">
                Inicia sesión para gestionar inspecciones, pedidos y reportes.
              </Typography>
            </Box>

            {mutation.error ? (
              <Alert severity="error">
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
                  label="Usuario"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
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
                  label="Contraseña"
                  type="password"
                  error={Boolean(fieldState.error)}
                  helperText={fieldState.error?.message}
                />
              )}
            />

            <Button
              variant="contained"
              size="large"
              onClick={form.handleSubmit(async (values) => {
                await mutation.mutateAsync(values);
              })}
              disabled={mutation.isPending}
            >
              Entrar
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
