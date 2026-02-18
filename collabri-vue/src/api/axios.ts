import axios from "axios";
import { useAuthStore } from "../stores/auth.store";

const axiosInstance = axios.create({
    baseURL: "http://localhost:8222/api/v1",
    withCredentials: true,
});

axiosInstance.interceptors.request.use((config) => {
    const authStore = useAuthStore();
    const token = authStore.accessToken;

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

axiosInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const authStore = useAuthStore();
        const originalRequest = error.config;

        // Only handle 401 errors and avoid infinite retry loops
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const response = await axiosInstance.post("/users/refresh", {});
                
                authStore.setAccessToken(response.data.accessToken);
                
                // Retry the original request with new token
                originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`;
                return axiosInstance(originalRequest);

            } catch (refreshError) {
                authStore.clearAccessToken();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default axiosInstance;