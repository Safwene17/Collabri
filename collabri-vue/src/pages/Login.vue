<script lang="ts" setup>
import { ref } from 'vue';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { LoginSchema, validateInputs } from '../utils/validation';
import router from '../router/main.route';
import { AuthService } from '../services/auth.service';
import { useAuthStore } from '../stores/auth';
import { handleRTAndValidationErrors } from '../utils/utils';

    defineOptions({
        name: "Login",
        components: {
            Toast,
        }
    });

    // Setup
    const toast = useToast();

    // Data
    const isSubmitting = ref(false);
    const userEmail = ref("");
    const userPassword = ref("");

    // Function to Handle Login Requests
    async function loginUser() {
        if (isSubmitting.value) return;

        const isValid = await validateInputs(
            LoginSchema, 
            {
                email: userEmail.value, 
                password: userPassword.value, 
            },
            toast 
        );

        if(!isValid) return;

        isSubmitting.value = true;

        try {
            // Authentication Service Instance
            const authService = new AuthService(userEmail.value, userPassword.value);
            // Auth Store Instance
            const authStore = useAuthStore();

            const loginResponse = await authService.login("http://localhost:8222/api/v1/auth/login");
            
            if(loginResponse.status === 200) {
                authStore.setAccessToken(loginResponse.data.data.access_token);

                router.push("/home");
            }
        } catch(error: any) {
            console.error("Unexpected Error in Login User: ", error);

            handleRTAndValidationErrors(error, toast);

        } finally {
            isSubmitting.value = false;
        }
    };
</script>

<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] w-full h-screen">
        <div class="grid lg:grid-cols-2 p-4 md:p-0 w-full h-screen">
            <!-- Image Container -->
            <div class="lg:block hidden w-full h-full">
                <img 
                    src="/assets/login-cover.png" 
                    alt="Login Cover"
                    class="object-cover bg-cover h-full lg:block sm:hidden" 
                />
            </div>

            <!-- Login Form Container -->
            <div class="flex items-center justify-center w-full h-full">
                <!-- Login Form -->
                <div class="flex flex-col gap-5">
                    <!-- Heading -->
                    <div class="text-center">
                        <h1 class="text-xl font-bold">Login to your account</h1>
                        <p class="text-sm text-gray-400 mt-2">
                            Enter your email below to login to your account
                        </p>
                    </div>

                    <!-- Email Input -->
                    <div class="flex flex-col gap-2">
                        <label for="email" class="font-semibold text-sm">Email :</label>
                        <InputText 
                            name="email" 
                            type="email" 
                            placeholder="user@example.com" 
                            class="text-black"
                            v-model="userEmail"
                        />
                    </div>

                    <!-- Password Input -->
                    <div class="flex flex-col gap-2">
                        <label for="password" class="font-semibold text-sm">Password :</label>
                        <InputText 
                            name="password" 
                            type="password" 
                            class="text-black"
                            placeholder="Your Password"
                            v-model="userPassword"
                        />
                    </div>

                    <!-- Sign In Button -->
                    <Button 
                        type="button" 
                        severity="contrast" 
                        label="Sign in" 
                        :disabled="isSubmitting"
                        @click="loginUser"
                        :loading="isSubmitting"
                    />

                    <!-- Forgot Password Link -->
                    <RouterLink to="/forgot-password" class="text-xs underline font-semibold">
                        Forgot Password ?
                    </RouterLink>

                    <!-- Separator -->
                    <div class="relative h-px w-full bg-[#373737] mt-4">
                        <span class="absolute top-1/2 translate-y-[-60%] left-1/2 translate-x-[-50%] bg-[#0a0a0a] text-white text-sm font-semibold p-1">
                            OR
                        </span>
                    </div>

                    <!-- Social Buttons -->
                    <Button type="submit" severity="secondary" label="Continue with Google" icon="fa-brands fa-google" />
                    <Button type="submit" severity="secondary" label="Continue with GitHub" icon="fa-brands fa-github" />

                    <!-- Sign Up Link -->
                    <a href="#" class="text-sm text-center font-light">
                        Don't have an account ? 
                        <RouterLink to="/register" class="underline font-semibold">Join now !</RouterLink>
                    </a>
                </div>
            </div>
        </div>
    </div>

    <Toast />
</template>