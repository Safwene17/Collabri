// src/hooks/shared/useProvideAuth.ts
import { useCallback, useEffect, useState } from "react";
import { api } from "@/services/api/client";
import * as authSvc from "@/services/frontoffice/auth";
import type { User } from "@/types/user";

export function useProvideAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [loading, setLoading] = useState(false);

  const fetchMe = useCallback(async () => {
    try {
      const res = await api.get<User>("/api/v1/users/me");
      setUser(res.data);
    } catch {
      setUser(null);
    }
  }, []);

  // restore session on mount
  useEffect(() => {
    (async () => {
      try {
        await authSvc.refresh();
        await fetchMe();
      } catch {
        setUser(null);
      } finally {
        setInitializing(false);
      }
    })();
  }, [fetchMe]);

  const login = useCallback(async (email: string, password: string) => {
    setLoading(true);
    try {
      await authSvc.login(email, password);
      await fetchMe();
    } finally {
      setLoading(false);
    }
  }, [fetchMe]);

  const signup = useCallback(async (payload: { firstname: string; lastname: string; email: string; password: string }) => {
    setLoading(true);
    try {
      await authSvc.signup(payload);
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setLoading(true);
    try {
      await authSvc.logout();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      await authSvc.refresh();
      await fetchMe();
    } finally {
      setLoading(false);
    }
  }, [fetchMe]);

  return {
    user,
    isAuthenticated: !!user,
    initializing,
    loading,
    login,
    signup,
    logout,
    refresh,
    fetchMe,
  } as const;
}
