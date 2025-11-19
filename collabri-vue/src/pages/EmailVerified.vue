<script lang="ts" setup>
import { RouterLink, useRoute } from 'vue-router';
import {  onMounted, ref } from 'vue';
import { AuthService } from '../services/auth.service';

    defineOptions({
        name: "EmailVerified"
    });

    const route = useRoute();

    // Data
    const isVerifying = ref(false);
    const verificationSuccess = ref(false);
    const verificationFailed = ref(false);

    // On Mounted
    onMounted(verifyEmail);

    // Function to Get Token & Use it to verify the email (if Token is valid)
    async function verifyEmail() {
        if(isVerifying.value) return;

        isVerifying.value = true;
        verificationFailed.value = false;
        verificationSuccess.value = false;

        try {
            const token = route.query.token as string || "";
            const authService = new AuthService();

            if(!token) {
                verificationFailed.value = true;
                return;
            }

            // Request
            const verifyResponse = await authService.verifyEmail(
                "http://localhost:8222/api/v1/auth/verify-email", 
                token
            );

            if(verifyResponse.status === 200 || verifyResponse.status === 201) {
                verificationSuccess.value = true;
                setTimeout(() => window.location.href = "/login", 2000);
            }
        } catch(error) {
            console.error("Error in Verify Email: ", error);

            verificationFailed.value = true;

        } finally {
            isVerifying.value = false;
        }
    };
</script>


<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] flex items-center justify-center p-4 w-full h-screen">
        <div 
            class="bg-[#101010] rounded-lg p-4 max-w-[500px] min-w-[320px]"
            style="border: 1px solid #888;"
        >
            <!-- Email Icon -->
            <div class="relative text-center">
                <i class="fa-solid fa-envelope text-8xl"></i>
                <!-- Success Icon -->
                <i 
                    v-if="verificationSuccess"
                    class="fa-solid fa-circle-check text-3xl absolute left-1/2 translate-x-[-50%] top-1/2 -translate-y-full text-green-500"
                ></i>

                <i 
                    v-if="verificationFailed"
                    class="fa-solid fa-circle-xmark text-3xl absolute left-1/2 translate-x-[-50%] top-1/2 -translate-y-full text-red-500"
                ></i>
            </div>

            <!-- Loading State Content -->
            <div 
                v-if="isVerifying"
                class="flex flex-col gap-2 items-center justify-center mt-4"
            >
                <ProgressSpinner 
                    style="width: 30px; height: 30px" strokeWidth="8" fill="transparent"
                    animationDuration=".8s" aria-label="Custom ProgressSpinner" 
                />
                <span class="font-light">Please wait...</span>
            </div>

            <!-- Successful Verification Content -->
            <div 
                v-if="verificationSuccess"
                class="flex flex-col items-center gap-3"
            >
                <!-- Message -->
                <h1 class="text-3xl font-extralight">Congratulations !</h1>
                <p class="text-center font-extralight">
                    Your account is now verified, you can access it now and enjoy your experience on our app.
                </p>

                <!-- Sign In Button -->
                <RouterLink 
                    to="/login"
                    class="text-black bg-white rounded-md py-2 px-3 text-sm font-semibold mt-2"
                >
                    <i class="fa-solid fa-right-to-bracket mr-2"></i>
                    <span>Sign In</span>
                </RouterLink>
            </div>

            <!-- Failed Verification Content -->
            <div
                v-if="verificationFailed"
                class="flex flex-col items-center gap-3"
            >
                <!-- Message -->
                <h1 class="text-3xl font-extralight">Something went wrong...</h1>
                <p class="text-center font-extralight">
                    You are seeing this because an error occurred while trying to verify your email. There is no problem, 
                    you still can verify it by clicking the button below.
                </p>

                <!-- Sign In Button -->
                <RouterLink 
                    to="/login"
                    class="text-black bg-white rounded-md py-2 px-3 text-sm font-semibold mt-2"
                >
                    <i class="fa-solid fa-arrow-rotate-right mr-2"></i>
                    <span>Resend Verification</span>
                </RouterLink>
            </div>
        </div>
    </div>
</template>