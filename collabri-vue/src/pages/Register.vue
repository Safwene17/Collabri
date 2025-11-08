<script lang="ts" setup>
import { ref } from 'vue';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { RegisterSchema, validateInputs } from '../utils/validation';
import { registerRequest } from '../services/auth';

    defineOptions({
        name: "Register"
    });

    // Setup
    const toast = useToast();

    // Data
    const isSubmitting = ref(false);

    const firstname = ref("");
    const lastname = ref("");
    const userEmail = ref("");
    const password = ref("");

    // Function to send a Request to Register a User
    async function registerUser() {
        if (isSubmitting.value) return;

        const isValid = await validateInputs(
            RegisterSchema,
            { 
                firstname: firstname.value, 
                lastname: lastname.value, 
                email: userEmail.value, 
                password: password.value
            },
            toast
        );

        if(!isValid) {
            return;
        }

        isSubmitting.value = true;

        try {
            await registerRequest({
                firstname: firstname.value,
                lastname: lastname.value,
                email: userEmail.value,
                password: password.value,
                url: "http://localhost:8222/api/v1/users/register",
                toast: toast
            });

        } catch(error) {
            console.error("Error in Register User: ", error);

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
            <form class="flex items-center justify-center w-full h-full">
                <!-- Login Form -->
                <div class="flex flex-col gap-5">
                    <!-- Heading -->
                    <div class="text-center">
                        <h1 class="text-xl font-bold">Create an account</h1>
                        <p class="text-sm text-gray-400 mt-2">
                            Enter your informations and become a member now !
                        </p>
                    </div>

                    <!-- Firstname & Lastname Container -->
                    <div class="flex justify-between gap-3 flex-wrap">
                        <!-- Firstname Input -->
                        <div class="flex flex-col gap-2 w-full">
                            <label for="firstname" class="font-semibold text-sm">Firstname :</label>
                            <InputText 
                                name="firstname" 
                                type="text" 
                                placeholder="John" 
                                class="text-black w-full"
                                v-model="firstname"
                                required="true"
                            />
                        </div>

                        <!-- Lastname Input -->
                        <div class="flex flex-col gap-2 w-full">
                            <label for="lastname" class="font-semibold text-sm">Lastname :</label>
                            <InputText 
                                name="lastname" 
                                type="text" 
                                placeholder="John" 
                                class="text-black w-full"
                                v-model="lastname"
                                required="true"
                            />
                        </div>
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
                            required="true"
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
                            v-model="password"
                            required="true"
                        />
                    </div>

                    <!-- Sign up Button -->
                    <Button 
                        type="button"
                        severity="contrast" 
                        label="Sign up" 
                        @click="registerUser"
                        :loading="isSubmitting"
                    />

                    <!-- Sign in Link -->
                    <a href="#" class="text-sm text-center font-light">
                        Already a member ? 
                        <RouterLink to="/login" class="underline font-semibold">Sign in !</RouterLink>
                    </a>
                </div>
            </form>
        </div>
    </div>

    <Toast />
</template>