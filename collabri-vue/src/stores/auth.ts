import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useAuthStore = defineStore("auth", () => {
    // State
    const accessToken = ref<string | null>(null);
    const loading = ref(false);

    // Getters
    const isAuthenticated = computed(() => !!accessToken.value);

    // Actions
    const setAccessToken = (token: string) => {
        if (token && token.trim() !== "") {
            accessToken.value = token;
            loading.value = false;
        } else {
            clearAccessToken();
        }
    };

    
    const clearAccessToken = () => {
        accessToken.value = null;
        loading.value = false;
    };

    const setLoading = (isLoading: boolean) => {
        loading.value = isLoading;
    };

    return {
        // State
        accessToken,
        loading,
        
        // Getters
        isAuthenticated,
        
        // Actions
        setAccessToken,
        clearAccessToken,
        setLoading,
    };
}, {
    persist: {
        key: "auth-storage",
    }
});