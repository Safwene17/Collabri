<script lang="ts" setup>
import { RouterLink } from 'vue-router';

    defineOptions({
        name: "LinkExpired"
    });
</script>

<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] flex items-center justify-center p-4 w-full h-screen">
        <div 
            class="bg-[#101010] rounded-lg p-4 max-w-[500px] min-w-[320px]"
            style="border: 1px solid #888;"
        >
            <!-- Icon & Title -->
            <div class="text-center">
                <i class="fa-regular fa-circle-xmark text-red-500 text-4xl"></i>
                <h1 class="text-xl mt-2 font-semibold">Link Expired</h1>
            </div>

            <!-- Paragraph -->
            <p class="text-center font-light mt-4">
                This link has expired or has already been used. 
                For your information, links are only valid for a limited time. 
                Please request a new one to continue with your account setup.
            </p>

            <div class="flex items-center justify-center mt-4">
                <RouterLink to="/home" class="flex items-center justify-center bg-white p-2 rounded-lg">
                    <i class="pi pi-home text-black"></i>
                </RouterLink>
            </div>
        </div>
    </div>
</template>