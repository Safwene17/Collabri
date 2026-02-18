<script lang="ts" setup>
import Menu from 'primevue/menu';
import { ref } from 'vue';
import { AuthService } from '../services/auth.service';

    defineOptions({
        name: "Header"
    });

    // SETUP
    const authService = new AuthService();

    // DATA
    const menu = ref();
    const isLoading = ref(false);
    const items = ref([
        {
            label: 'Logout',
            icon: 'pi pi-sign-out',
            command: () => logout(),
        }
    ]);


    // METHODS
    const toggle = (event: any) => {
        menu.value.toggle(event);
    };

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

<template>
    <header class="p-4 shadow-md bg-white text-black">
        <nav class="flex items-center mx-2">
            <!-- Logo -->
            <h3 class="font-semibold text-xl flex-1">Collabri</h3>

            <div class="flex justify-between items-center gap-8">
                <!-- Nav Links -->
                <ul class="flex items-center gap-4">
                    <li>
                        <RouterLink to="/home/main">Home</RouterLink>
                    </li>
                    <li>
                        Contact
                    </li>
                    <li>
                        About
                    </li>
                </ul>

                <!-- Profile Dropdown -->
                <div>
                    <span 
                        @click="toggle"
                        class="flex items-center gap-2 border p-2 rounded-2xl cursor-pointer"
                    >
                        <i class="fa-regular fa-circle-user text-2xl"></i>
                        <i class="fa-solid fa-caret-down"></i>
                    </span>
                    <Menu ref="menu" id="overlay_menu" :model="items" :popup="true" />
                </div>
            </div>
        </nav>
    </header>
</template>