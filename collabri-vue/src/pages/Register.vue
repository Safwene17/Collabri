<script lang="ts" setup>
import { ref } from 'vue';
import axios from 'axios';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { useRegle } from '@regle/core';
import { required, email, minLength, maxLength } from '@regle/rules';

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

    // Validation setup
    const { r$ } = useRegle(
        {
            firstname,
            lastname,
            userEmail,
            password
        },
        {
            firstname: {
                required,
                minLength: minLength(3),
                maxLength: maxLength(255)
            },
            lastname: {
                required,
                minLength: minLength(3),
                maxLength: maxLength(255)
            },
            userEmail: {
                required,
                email
            },
            password: {
                required,
                minLength: minLength(12),
                maxLength: maxLength(255)
            }
        }
    );

    // Function to send a Request to Register a User
    async function registerUser() {
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
            const registerResponse = await axios.post("http://localhost:8222/api/v1/users/register", {
                    firstname: firstname.value,
                    lastname: lastname.value,
                    email: userEmail.value,
                    password: password.value,
                },
                {
                headers: {
                    "Content-Type": "application/json",
                }
            });

            // Success Response
            if(registerResponse.status === 200) {
                console.log(registerResponse.data);

                toast.add({
                    severity: "success",
                    summary: "Success",
                    detail: "Compte créé avec succès", // Fixed accent
                    life: 3000
                });
            }

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