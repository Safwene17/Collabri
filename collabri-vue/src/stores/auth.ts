import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
    state: () => ({
        accessToken: null as string | null,
        isAuthenticated: false,
        loading: false
    }),

    actions: {
        setAccessToken(token: string) {
            if(token || token.trim() !== "") {
                this.accessToken = token;
                this.isAuthenticated = true;
                this.loading = false;
            } else {
                this.accessToken = null;
                this.isAuthenticated = false;
                this.loading = false;
            }
        },

        clearAccessToken() {
            this.accessToken = null;
            this.isAuthenticated = false;
            this.loading = false;
        },
    },

    persist: true,
});
