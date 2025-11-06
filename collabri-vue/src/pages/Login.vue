<script lang="ts" setup>
import { ref } from 'vue';
import axios from 'axios';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { useRegle } from '@regle/core';
import { required, email, minLength, maxLength } from '@regle/rules';

    defineOptions({
        name: "Login"
    });

    // Setup
    const toast = useToast();

    // Data
    const isSubmitting = ref(false);
    const userEmail = ref("");
    const userPassword = ref("");

    // Validation setup
    const { r$ } = useRegle(
        {
            userEmail,
            userPassword
        },
        {
            userEmail: {
                required,
                email
            },
            userPassword: {
                required,
                minLength: minLength(12),
                maxLength: maxLength(255)
            }
        }
    );

    async function loginUser() {
        if (isSubmitting.value) return;

        // Trigger full validation and check
        const isValid = await r$.$validate();
        if (!isValid) {
            toast.add({
                severity: "warn",
                summary: "Validation Error",
                detail: "Please fix the errors in the form before submitting.",
                life: 3000
            });
            return;
        }

        isSubmitting.value = true;

        try {
            const loginResponse = await axios.post("http://localhost:8222/api/v1/users/login", {
                    email: userEmail.value,
                    password: userPassword.value,
                },
                {
                headers: {
                    "Content-Type": "application/json",
                }
            });

            // Success Response
            if(loginResponse.status === 200) {
                console.log(loginResponse.data);

                toast.add({
                    severity: "success",
                    summary: "Success",
                    detail: "Logged in successfully", // Updated message for login context
                    life: 3000
                });
            }

        } catch(error) {
            console.error("Error in Login User: ", error);

            toast.add({
                severity: "error",
                summary: "Error",
                detail: "Une erreur s'est produite. Réessayer plus tard",
                life: 3000
            });

        } finally {
            isSubmitting.value = false;
        }
    };
</script>

<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] w-full h-screen">
        <div class="grid grid-cols-2 w-full h-screen">
            <!-- Image Container -->
            <div class="w-full h-full">
                <img 
                    src="/assets/login-cover.png" 
                    alt="Login Cover"
                    class="object-cover bg-cover h-full lg:block sm:hidden" 
                />
            </div>

            <!-- Login Form Container -->
            <form class="flex items-center justify-center w-full h-full">
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
                        <label for="email" class="font-semibold text-sm">Password :</label>
                        <InputText 
                            name="email" 
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
                        @click="loginUser"
                    />

                    <!-- Forgot Password Link -->
                    <a href="#" class="text-xs underline font-semibold">
                        Forgot Password ?
                    </a>

                    <!-- Separator -->
                    <div class="relative h-[1px] w-full bg-[#373737] mt-4">
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
            </form>
        </div>
    </div>

    <Toast />
</template>