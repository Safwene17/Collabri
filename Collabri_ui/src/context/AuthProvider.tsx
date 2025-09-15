// src/context/AuthProvider.tsx
import React, { createContext, useContext } from "react";
import { useProvideAuth } from "@/hooks/shared/useProvideAuth";

type AuthValue = ReturnType<typeof useProvideAuth>;

const AuthContext = createContext<AuthValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const auth = useProvideAuth();
  return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
};

// consumer hook: use this in components
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
