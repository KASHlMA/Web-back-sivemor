import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertMessage } from "../../components/AdminPrimitives";
import { schemaHelpers } from "../../components/ResourceTablePage";
import { brandAssets } from "../../lib/brand";
import { useAuth } from "../../lib/session";

const schema = z.object({
  username: schemaHelpers.requiredText("El usuario"),
  password: schemaHelpers.requiredText("La contrasena")
});

export function LoginPage() {
  const navigate = useNavigate();
  const { session, login } = useAuth();
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: {
      username: "admin",
      password: "Admin123!"
    }
  });

  const mutation = useMutation({
    mutationFn: async (values) => {
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
    <div className="grid min-h-screen bg-[var(--page)] md:grid-cols-[0.58fr_1fr]">
      <div className="relative hidden overflow-hidden bg-[#557665] md:block">
        <div
          className="absolute inset-0 bg-[var(--header-accent)]"
          style={{ clipPath: "polygon(18% 0, 100% 0, 60% 100%, 0 100%)" }}
        />
      </div>

      <div className="flex items-center justify-center px-6 py-10 md:px-10">
        <div className="w-full max-w-[408px] text-center">
          <img src={brandAssets.logoDark} alt="SIVEMOR" className="mx-auto w-[234px] max-w-[78%]" />

          <div className="mt-4">
            <h1 className="text-[2.05rem] font-bold text-[var(--title)]">SIVEMOR</h1>
            <p className="mt-1 text-sm text-[var(--title)]">Sistema de Verificacion de Morelos</p>
          </div>

          <h2 className="mt-8 text-[2rem] font-semibold text-[var(--title)]">Iniciar sesion</h2>

          <div className="mt-6 text-left">
            <AlertMessage
              message={
                mutation.error
                  ? mutation.error instanceof Error
                    ? mutation.error.message
                    : "No fue posible iniciar sesion"
                  : null
              }
            />
          </div>

          <form
            className="mt-6 space-y-5"
            onSubmit={form.handleSubmit(async (values) => {
              await mutation.mutateAsync(values);
            })}
          >
            <Controller
              name="username"
              control={form.control}
              render={({ field, fieldState }) => (
                <div className="text-left">
                  <input {...field} placeholder="Correo electronico" className="field-base min-h-[51px]" />
                  {fieldState.error ? (
                    <p className="mt-1 text-xs font-semibold text-[var(--danger)]">{fieldState.error.message}</p>
                  ) : null}
                </div>
              )}
            />

            <Controller
              name="password"
              control={form.control}
              render={({ field, fieldState }) => (
                <div className="text-left">
                  <input {...field} type="password" placeholder="Contrasena" className="field-base min-h-[51px]" />
                  {fieldState.error ? (
                    <p className="mt-1 text-xs font-semibold text-[var(--danger)]">{fieldState.error.message}</p>
                  ) : null}
                </div>
              )}
            />

            <button
              type="submit"
              disabled={mutation.isPending}
              className="mx-auto inline-flex min-h-[37px] w-[158px] items-center justify-center rounded-md bg-[#97aaa0] px-4 text-sm font-semibold text-white transition hover:bg-[#7f9589] disabled:cursor-not-allowed disabled:opacity-60"
            >
              Iniciar sesion
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
