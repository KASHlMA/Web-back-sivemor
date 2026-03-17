import { API_BASE_URL } from "./config";
import {
  clearStoredSession,
  getStoredSession,
  setStoredSession,
  type AuthSession
} from "./sessionStorage";

type ApiRequestInit = Omit<RequestInit, "body"> & {
  body?: unknown;
  skipAuth?: boolean;
};

let refreshPromise: Promise<AuthSession | null> | null = null;

async function refreshSession(): Promise<AuthSession | null> {
  const session = getStoredSession();
  if (!session?.refreshToken) {
    clearStoredSession();
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ refreshToken: session.refreshToken })
    })
      .then(async (response) => {
        if (!response.ok) {
          clearStoredSession();
          return null;
        }

        const refreshed = (await response.json()) as AuthSession;
        setStoredSession(refreshed);
        return refreshed;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function rawRequest<T>(
  path: string,
  { body, headers, skipAuth, ...rest }: ApiRequestInit = {},
  retryOnUnauthorized = true
): Promise<T> {
  const session = getStoredSession();
  const requestHeaders = new Headers(headers);

  let requestBody: BodyInit | undefined;
  if (body instanceof FormData) {
    requestBody = body;
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    requestBody = JSON.stringify(body);
  }

  if (!skipAuth && session?.accessToken) {
    requestHeaders.set("Authorization", `Bearer ${session.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: requestBody
  });

  if (response.status === 401 && retryOnUnauthorized && !skipAuth) {
    const refreshed = await refreshSession();
    if (refreshed) {
      return rawRequest(path, { body, headers, skipAuth, ...rest }, false);
    }
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => undefined);
    const message =
      payload?.message ??
      payload?.error ??
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return (await response.json()) as T;
}

export const api = {
  login: (username: string, password: string) =>
    rawRequest<AuthSession>(
      "/auth/login",
      {
        method: "POST",
        body: { username, password },
        skipAuth: true
      },
      false
    ),
  logout: (refreshToken: string) =>
    rawRequest<{ success: boolean }>(
      "/auth/logout",
      {
        method: "POST",
        body: { refreshToken },
        skipAuth: true
      },
      false
    ),
  get: <T>(path: string) => rawRequest<T>(path),
  post: <T>(path: string, body: unknown) =>
    rawRequest<T>(path, { method: "POST", body }),
  put: <T>(path: string, body: unknown) =>
    rawRequest<T>(path, { method: "PUT", body }),
  delete: (path: string) => rawRequest<void>(path, { method: "DELETE" })
};
