<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { resetPasswordSchema, validateInputs } from '../utils/validation';
import { useToast } from 'primevue/usetoast';
import Toast from 'primevue/toast';
import { AuthService } from '../services/auth.service';
import { handleRTAndValidationErrors } from '../utils/utils';
import router from '../router/main.route';

    defineOptions({
        name: "ResetPassword",
        components: {
            Toast
        }
    });

    // Setup
    const toast = useToast();

    // Data
    const token = ref("");
    const newPassword = ref("");
    const successMessage = ref("");

    const showSuccessMessage = ref(false);
    const isSubmitting = ref(false);

    // On Mounted
    onMounted(() => {
        // Check if token exists & get it
        const router = useRoute();
        token.value = router.query.token as string | "";

        if(!token || token.value === "") {
            window.location.href = "/login";
        }
    });

    // Reset Password Request
    async function resetPassword() {
        if(isSubmitting.value) return;

        // Input Validation
        const isValid = await validateInputs(resetPasswordSchema, {
            newPassword: newPassword.value
        },
            toast
        );

        if(!isValid) return;

        // Start loading state
        isSubmitting.value = true;
        // Reset success message state to not let stick
        showSuccessMessage.value = false;
        // Reset success message value to empty state
        successMessage.value = "";
        try {
            const authService = new AuthService();

            // Request
            const resetPasswordResponse = await authService.resetPassword(
                "http://localhost:8222/api/v1/auth/reset-password",
                token.value,
                newPassword.value
            );

            // Success response
            if(resetPasswordResponse.status === 200) {
                // Show success message
                successMessage.value = resetPasswordResponse.data.message || "Password was updated successfully";
                showSuccessMessage.value = true;
                // Reset password value
                newPassword.value = "";

                setTimeout(() => router.push("/login"), 1500);
            }

        } catch(error: any) {
            console.error("Error in Reset Password :", error);

            handleRTAndValidationErrors(error, toast);

        } finally {
            isSubmitting.value = false; // Reset loading state
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
            <h1 class="text-xl text-center font-bold text-white">Reset Password</h1>

            <div class="flex flex-col gap-2 mt-4">
                <!-- Paragraph -->
                <p class="text-sm text-[#e0e0e0]">
                    You can now enter your new password, make sure it is strong and follows the rules.
                    We are here to help you whenever you're in trouble.
                </p>

                <!-- Success Message -->
                <div 
                    v-if="showSuccessMessage"
                    class="flex items-center gap-2 p-4 rounded-lg bg-green-600 mt-2"
                >
                    <i class="fa-solid fa-check-circle"></i>
                    <span class="text-sm"> {{ successMessage }}</span>
                </div>

                <!-- Main Elements -->
                <div class="flex flex-col gap-2 mt-2">
                    <!-- Label -->
                    <label for="email" class="text-sm font-semibold">New Password :</label>
                    <!-- Email Input -->
                    <div class="relative w-full">
                        <i class="pi pi-lock absolute top-1/2 translate-y-[-50%] left-3"></i>
                        <input 
                            name="new-password"
                            type="password"
                            class="text-white py-2 pl-10 pr-2 rounded-lg w-full text-sm"
                            style="border: 1px solid #ddd;"
                            required="true"
                            v-model="newPassword"
                        />
                    </div>
                </div>

                <!-- Reset Button -->
                <Button 
                    type="button" 
                    severity="contrast" 
                    label="Confirm"
                    class="mt-2 text-sm"
                    :disabled="isSubmitting"
                    :loading="isSubmitting"
                    @click="resetPassword"
                />
            </div>
        </div>
    </div>
    <Toast />
</template>