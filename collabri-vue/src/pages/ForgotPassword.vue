<script lang="ts" setup>
import axios from 'axios';
import { ref } from 'vue';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { forgotPasswordSchema, validateInputs } from '../utils/validation';

    defineOptions({
        name: "ForgotPassword",
        components: {
            Toast,
        }
    });

    // Setup
    const toast = useToast();

    // Data
    const isSending = ref(false);
    const emailSuccess = ref(false);
    const emailFailed = ref(false);
    
    const resetEmail = ref("");

    // Function to Send a Reset Password Email
    async function sendResetEmail() {
        emailFailed.value = false;
        emailSuccess.value = false;

        if(isSending.value) return;

        const isValid = await validateInputs(
            forgotPasswordSchema,
            { email: resetEmail.value },
            toast
        );

        if(!isValid) {
            return;
        }

        isSending.value = true;

        try {
            const resetResponse = await axios.post(
                "http://localhost:8222/api/v1/users/forgot-password", 
                {
                    email: resetEmail.value
                },
                {
                    headers: {
                        "Content-Type": "application/json"
                    }
                }
            );

            // Success Response
            if(resetResponse.status === 200) {
                emailSuccess.value = true;
            }

        } catch(error: any) {
            console.error("Error in Send Reset Email: ", error);

            emailFailed.value = true;

        } finally {
            isSending.value = false;
        }
    };
</script>


<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] flex items-center justify-center p-4 w-full h-screen">
        <div 
            class="bg-[#101010] rounded-lg p-4 max-w-[500px]"
            style="border: 1px solid #888;"
        >
            <!-- Heading -->
            <h1 class="text-xl text-center font-bold text-white">Forgot Password ?</h1>

            <div class="flex flex-col gap-2 mt-4">
                <!-- Paragraph -->
                <p class="text-sm text-[#e0e0e0]">
                    Seems like you forgot your password for <strong>Collabri</strong>. 
                    If this is true, we'll help you reset your password.
                </p>

                <!-- Success Message -->
                <div 
                    v-if="emailSuccess"
                    class="flex items-center gap-2 p-4 rounded-lg bg-green-600 mt-2"
                >
                    <i class="fa-solid fa-check-circle"></i>
                    <span class="text-sm">An email was successfully sent to you. Please check your inbox</span>
                </div>

                <!-- Error Message -->
                <div 
                    v-if="emailFailed"
                    class="flex items-center gap-2 p-4 rounded-lg bg-red-600 mt-2"
                >
                    <i class="fa-solid fa-circle-exclamation"></i>
                    <span class="text-sm">An error occurred when sending an email. Try again later</span>
                </div>

                <div class="flex flex-col gap-2 mt-2">
                    <!-- Label -->
                    <label for="email" class="text-sm font-semibold">Email :</label>
                    <!-- Email Input -->
                    <div class="relative w-full">
                        <i class="pi pi-envelope absolute top-1/2 translate-y-[-50%] left-3"></i>
                        <input 
                            name="email"
                            type="email"
                            placeholder="user@example.com"
                            class="text-white py-2 pl-10 pr-2 rounded-lg w-full text-sm"
                            style="border: 1px solid #ddd;"
                            required="true"
                            v-model="resetEmail"
                        />
                    </div>
                </div>

                <!-- Reset Button -->
                <Button 
                    type="button" 
                    severity="contrast" 
                    label="Reset Password"
                    class="mt-2 text-sm"
                    :disabled="isSending"
                    :loading="isSending"
                    @click="sendResetEmail"
                />

                <!-- Login Navigation Button -->
                <RouterLink 
                    to="/login"
                    class="text-center font-light text-white underline mt-2"
                >
                    <i class="fa-solid fa-arrow-left mr-1"></i>
                    Back to Login
                </RouterLink>
            </div>
        </div>
    </div>

    <Toast />
</template>