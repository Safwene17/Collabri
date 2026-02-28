<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { CalendarService } from '../services/calendar.service';
import { safeApiCall } from '../services/helpers/apiHelper';
import { useToast } from 'primevue/usetoast';

    defineOptions({
        name: "MainPage"
    });

    // SETUP
    const calendarService = new CalendarService();
    const toast = useToast();

    // DATA
    const isLoading = ref(false);
    const errorMessage = ref(null);
    const allCalendars = ref([]);

    // ON MOUNTED
    onMounted(getAllCalendars);

    // METHODS
    async function getAllCalendars() {
        errorMessage.value = null;

        if(isLoading.value) return;

        isLoading.value = true;

        try {
            // Request
            const getResponse = await calendarService.getPublicCalendars();

            if(getResponse.status === 200) {
                allCalendars.value = getResponse.data;
            }
        } catch(error: any) {
            console.error("Error when Fetching Calendars: ", error);

            if(error.message) {
                errorMessage.value = error.message;
            }
        } finally {
            isLoading.value = false;
        }
    };

</script>

<template>
    <div class="h-full p-4">
        <!-- Loading Screen -->
        <div 
            v-if="isLoading"
            class="h-full flex items-center justify-center"
        >
            <ProgressSpinner style="width: 35px; height: 35px" strokeWidth="8" fill="transparent"
                animationDuration=".5s" aria-label="Custom ProgressSpinner" />
        </div>

        <!-- Error Message -->
        <div
            v-else-if="errorMessage != null"
            class="h-full flex items-center justify-center"
        >
            <div class="p-4 rounded-lg bg-red-500 text-center min-w-[200px]">
                <i class="fa-regular fa-circle-xmark text-2xl mb-2"></i>
                <p class="font-semibold text-md">{{ errorMessage }}</p>
            </div>
        </div>

        <!-- Calendars Section -->
        <div
            v-else
        >
            <p>data here</p>
        </div>
    </div>
</template>