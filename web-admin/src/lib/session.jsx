import { createContext, useContext, useMemo, useState } from "react";
import { api } from "./api";
import {
  clearStoredSession,
  getStoredSession,
  setStoredSession
} from "./sessionStorage";

const AuthContext = createContext(undefined);

function isAdminSession(session) {
  return session?.user?.role === "ADMIN";
}

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(() => {
    const storedSession = getStoredSession();
    return isAdminSession(storedSession) ? storedSession : null;
  });

  const setSession = (value) => {
    const nextValue = isAdminSession(value) ? value : null;
    setSessionState(nextValue);
    setStoredSession(nextValue);
  };

  const login = async (username, password) => {
    const nextSession = await api.login(username, password);
    if (!isAdminSession(nextSession)) {
      clearStoredSession();
      setSessionState(null);
      throw new Error("Este panel solo permite usuarios administradores");
    }
    setSession(nextSession);
  };

  const logout = async () => {
    if (session?.refreshToken) {
      await api.logout(session.refreshToken).catch(() => undefined);
    }
    clearStoredSession();
    setSessionState(null);
  };

  const value = useMemo(
    () => ({
      session,
      login,
      logout,
      setSession,
      isAdmin: isAdminSession(session)
    }),
    [session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
