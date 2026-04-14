<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { CalendarService } from '../services/calendar.service';

interface CalendarEvent {
    id: string;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    location: string;
}

interface CalendarMember {
    id: string;
    displayName: string;
    role: string;
}

interface CalendarTask {
    id: string;
    title: string;
    taskStatus: string;
}

interface CalendarItem {
    id: string;
    name: string;
    description: string;
    visibility: string;
    timeZone: string;
    members: CalendarMember[];
    tasks: CalendarTask[];
    events: CalendarEvent[];
}

    defineOptions({
        name: "MainPage"
    });

    // SETUP
    const calendarService = new CalendarService();

    // DATA
    const isLoading = ref(false);
    const errorMessage = ref<string | null>(null);
    const allCalendars = ref<CalendarItem[]>([]);

    // // ON MOUNTED
    onMounted(getAllCalendars);

    // // METHODS
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

    function formatDateTime(value: string) {
        if(!value) return 'N/A';

        const date = new Date(value);

        if(Number.isNaN(date.getTime())) {
            return value;
        }

        return new Intl.DateTimeFormat('en-GB', {
            year: 'numeric',
            month: 'short',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
        }).format(date);
    }

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
            class="space-y-4"
        >
            <div class="flex items-center justify-between">
                <h2 class="text-xl font-bold">Public Calendars</h2>
                <span class="text-sm text-gray-400">{{ allCalendars.length }} found</span>
            </div>

            <div
                v-if="allCalendars.length === 0"
                class="rounded-lg border border-surface-700 p-4 text-center text-gray-400"
            >
                No calendars available.
            </div>

            <div
                v-else
                class="grid grid-cols-1 gap-4 lg:grid-cols-2"
            >
                <article
                    v-for="calendar in allCalendars"
                    :key="calendar.id"
                    class="rounded-xl border border-surface-700 bg-surface-900 p-4"
                >
                    <div class="mb-3 flex items-start justify-between gap-3">
                        <div>
                            <h3 class="text-lg font-semibold">{{ calendar.name }}</h3>
                            <p class="text-sm text-gray-400">{{ calendar.description }}</p>
                        </div>
                        <span class="rounded-full bg-green-700/30 px-2 py-1 text-xs font-semibold text-green-300">
                            {{ calendar.visibility }}
                        </span>
                    </div>

                    <div class="mb-3 grid grid-cols-2 gap-2 text-sm">
                        <p><span class="font-medium">Timezone:</span> {{ calendar.timeZone }}</p>
                        <p><span class="font-medium">Members:</span> {{ calendar.members?.length || 0 }}</p>
                        <p><span class="font-medium">Tasks:</span> {{ calendar.tasks?.length || 0 }}</p>
                        <p><span class="font-medium">Events:</span> {{ calendar.events?.length || 0 }}</p>
                    </div>

                    <div>
                        <p class="mb-2 text-sm font-semibold">Latest Events</p>

                        <div
                            v-if="!calendar.events || calendar.events.length === 0"
                            class="text-sm text-gray-400"
                        >
                            No events yet.
                        </div>

                        <ul
                            v-else
                            class="space-y-2"
                        >
                            <li
                                v-for="eventItem in calendar.events.slice(0, 3)"
                                :key="eventItem.id"
                                class="rounded-lg bg-surface-800 p-3"
                            >
                                <p class="font-medium">{{ eventItem.title }}</p>
                                <p class="text-sm text-gray-400">{{ eventItem.description }}</p>
                                <p class="mt-1 text-xs text-gray-400">
                                    {{ formatDateTime(eventItem.startTime) }} - {{ formatDateTime(eventItem.endTime) }}
                                </p>
                                <p class="text-xs text-gray-500">{{ eventItem.location || 'No location' }}</p>
                            </li>
                        </ul>
                    </div>
                </article>
            </div>
        </div>
    </div>
</template>