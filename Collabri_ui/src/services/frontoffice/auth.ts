// src/services/frontoffice/auth.ts
import { api } from "../api/client";
import { AuthEndpoints } from "../api/endpoints";

export async function login(email: string, password: string) {
  await api.post(AuthEndpoints.login, { email, password }); // sets httpOnly cookies on backend
}
export async function signup(payload: unknown) {
  await api.post(AuthEndpoints.register, payload);
}
export async function logout() {
  await api.post(AuthEndpoints.logout);
}
export async function refresh() {
  await api.post(AuthEndpoints.refresh);
}
