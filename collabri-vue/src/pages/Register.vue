<script lang="ts" setup>
import { ref } from 'vue';
import { AuthService } from '../services/auth.service';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { RegisterSchema, validateInputs } from '../utils/validation';
import { handleRTAndValidationErrors, togglePassword } from '../utils/utils';


    defineOptions({
        name: "Register"
    });

    // Setup
    const toast = useToast();

    // Data
    const isSubmitting = ref(false);
    const emailSuccessMessage = ref(false);

    const firstname = ref("");
    const lastname = ref("");
    const userEmail = ref("");
    const password = ref("");

    // Function to Reset all Fields Values
    function resetFields() {
        firstname.value = "";
        lastname.value = "";
        userEmail.value = "";
        password.value = "";
    };

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

        if(!isValid) return;

        isSubmitting.value = true;
        emailSuccessMessage.value = false;

        try {
            const authService = new AuthService(userEmail.value, password.value, firstname.value, lastname.value);

            const registerResponse = await authService.register("http://localhost:8222/api/v1/auth/register");

            if(registerResponse.status === 201 || registerResponse.status === 200) {
                resetFields();
                emailSuccessMessage.value = true;
            }
        } catch(error: any) {
            console.error("Error in Register: ", error);

            handleRTAndValidationErrors(error, toast);
            
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

                    <!-- Register Success Message -->
                    <div 
                        v-if="emailSuccessMessage"
                        class="flex items-center gap-2 p-4 rounded-lg bg-green-600 mt-2"
                    >
                        <i class="fa-solid fa-check-circle"></i>
                        <span class="text-sm">A verification link was sent to your email successfully.</span>
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
                        <div class="relative">
                            <InputText 
                                name="password" 
                                type="password" 
                                id="password-input"
                                class="text-black w-full"
                                placeholder="Your Password"
                                v-model="password"
                                required="true"
                            />
                            <!-- Hide & Show Password Icon -->
                            <i 
                                id="toggle-icon"
                                class="pi pi-eye absolute top-1/2 -translate-y-1/2 right-3 cursor-pointer"
                                @click="togglePassword('password-input', 'toggle-icon')"
                            ></i>
                        </div>
                    </div>

                    <!-- Sign up Button -->
                    <Button 
                        type="button"
                        severity="contrast" 
                        label="Sign up" 
                        @click="registerUser"
                        :loading="isSubmitting"
                        class="text-sm!"
                    />

                    <!-- Sign in Link -->
                    <a href="#" class="text-sm text-center font-light">
                        Already a member ? 
                        <RouterLink 
                            to="/login" 
                            class="underline font-semibold"
                        >
                            Sign in !
                        </RouterLink>
                    </a>
                </div>
            </form>
        </div>
    </div>

    <Toast />
</template>


<style scoped>
.p-inputtext {
    font-size: 14px !important;
}
</style>