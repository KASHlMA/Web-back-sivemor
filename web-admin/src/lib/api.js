import { API_BASE_URL } from "./config";
import {
  clearStoredSession,
  getStoredSession,
  setStoredSession
} from "./sessionStorage";

let refreshPromise = null;

async function refreshSession() {
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

        const refreshed = await response.json();
        setStoredSession(refreshed);
        return refreshed;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function rawRequest(path, { body, headers, skipAuth, ...rest } = {}, retryOnUnauthorized = true) {
  const session = getStoredSession();
  const requestHeaders = new Headers(headers);

  let requestBody;
  if (body instanceof FormData) {
    requestBody = body;
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    requestBody = JSON.stringify(body);
  }

  if (!skipAuth && session?.accessToken) {
    requestHeaders.set("Authorization", `Bearer ${session.accessToken}`);
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: requestHeaders,
      body: requestBody
    });
  } catch (error) {
    throw new Error("No fue posible conectar con el servidor. Intenta nuevamente en unos segundos.");
  }

  if (response.status === 401 && retryOnUnauthorized && !skipAuth) {
    const refreshed = await refreshSession();
    if (refreshed) {
      return rawRequest(path, { body, headers, skipAuth, ...rest }, false);
    }
  }

  if (response.status === 204) {
    return undefined;
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => undefined);
    const message =
      payload?.message ??
      payload?.error ??
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return response.json();
}

export const api = {
  login: (username, password) =>
    rawRequest(
      "/auth/login",
      {
        method: "POST",
        body: { username, password },
        skipAuth: true
      },
      false
    ),
  logout: (refreshToken) =>
    rawRequest(
      "/auth/logout",
      {
        method: "POST",
        body: { refreshToken },
        skipAuth: true
      },
      false
    ),
  get: (path) => rawRequest(path),
  post: (path, body) => rawRequest(path, { method: "POST", body }),
  put: (path, body) => rawRequest(path, { method: "PUT", body }),
  delete: (path) => rawRequest(path, { method: "DELETE" })
};
