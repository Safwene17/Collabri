<script lang="ts" setup>
import { ref } from 'vue';
import { AuthService } from '../services/auth.service';
    defineOptions({
        name: "Home"
    });

    // Data
    const authService = new AuthService();
    const isLoading = ref(false);

    // Logout Request Function
    async function logout() {
        if(isLoading.value) return;

        isLoading.value = true;

        try {
            await authService.logout("http://localhost:8222/api/v1/auth/logout");
        } catch(error: any) {
            console.error("Unexpected Error in Logout: ", error);
        } finally {
            isLoading.value = false;
        }
    };
</script>

<!-- This component is just for testing protected routes -->
<template>
    <div class="bg-black">
        <button 
            class="cursor-pointer bg-blue-500"
            @click="logout"
        >
            Logout
        </button>
        <h1 class="text-red-500">Hello</h1>
    </div>
</template>