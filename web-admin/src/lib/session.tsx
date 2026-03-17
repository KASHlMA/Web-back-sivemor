import {
  createContext,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
import { api } from "./api";
import {
  clearStoredSession,
  getStoredSession,
  setStoredSession,
  type AuthSession
} from "./sessionStorage";

type AuthContextValue = {
  session: AuthSession | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setSession: (session: AuthSession | null) => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSessionState] = useState<AuthSession | null>(() => getStoredSession());

  const setSession = (value: AuthSession | null) => {
    setSessionState(value);
    setStoredSession(value);
  };

  const login = async (username: string, password: string) => {
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
