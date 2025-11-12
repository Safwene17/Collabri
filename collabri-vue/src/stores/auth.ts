import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
    state: () => ({
        accessToken: "" as string | null,
    }),

    actions: {
        setAccessToken(token: string) {
            this.accessToken = token;
        },

        logout() {
            this.accessToken = null;
        },
    },
});
