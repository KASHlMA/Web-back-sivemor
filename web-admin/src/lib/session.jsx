import { createContext, useContext, useMemo, useState } from "react";
import { api } from "./api";
import {
  clearStoredSession,
  getStoredSession,
  setStoredSession
} from "./sessionStorage";

const AuthContext = createContext(undefined);

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(() => getStoredSession());

  const setSession = (value) => {
    setSessionState(value);
    setStoredSession(value);
  };

  const login = async (username, password) => {
    const nextSession = await api.login(username, password);
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
      setSession
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
