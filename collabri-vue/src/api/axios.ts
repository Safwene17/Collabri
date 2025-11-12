import axios from "axios";
import { useAuthStore } from "../stores/auth";

const api = axios.create({
    baseURL: "http://localhost:8222/api/v1",
    withCredentials: true,
});

// ✅ Automatically attach access token to every request
api.interceptors.request.use((config) => {
    const authStore = useAuthStore();
    const token = authStore.accessToken;

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

// ✅ Automatically refresh token on 401 errors
api.interceptors.response.use((response) => response,
    async (error) => {
        const authStore = useAuthStore();
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
            const { data } = await axios.post(
            "http://localhost:8222/api/v1/users/refresh",
            {},
            { withCredentials: true }
            );

            authStore.setAccessToken(data.accessToken);

            // Retry the original request with new token
            originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
            return api(originalRequest);

        } catch (refreshError) {
            authStore.logout();
            throw refreshError;
        }
        }

        throw error;
    }
);


export default api;