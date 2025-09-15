// src/services/api/interceptors.ts
import { api } from "@/services/api/client";
import { AuthEndpoints } from "@/services/api/endpoints";

let isRefreshing = false;
let failedQueue: Array<{ resolve: (v?: unknown) => void; reject: (e: unknown) => void; config: unknown }> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach(p => (error ? p.reject(error) : p.resolve(undefined)));
  failedQueue = [];
};

export function setupInterceptors() {
  api.interceptors.response.use(
    r => r,
    async (error) => {
      const originalRequest = error?.config;
      if (!originalRequest) return Promise.reject(error);

      if (error.response?.status === 401 && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject, config: originalRequest });
          }).then(() => api(originalRequest));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          // call refresh endpoint (backend must set new access cookie)
          await api.post(AuthEndpoints.refresh);
          processQueue(null);
          return api(originalRequest);
        } catch (err) {
          processQueue(err);
          return Promise.reject(err);
        } finally {
          isRefreshing = false;
        }
      }

      return Promise.reject(error);
    }
  );
}
