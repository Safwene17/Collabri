<script lang="ts" setup>
    defineOptions({
        name: "ForgotPassword"
    });


</script>

<!-- Component Template -->
<template>
    <div class="bg-[#0a0a0a] flex items-center justify-center p-4 w-full h-screen">
        <div 
            class="bg-[#101010] rounded-lg p-4 max-w-[500px]"
            style="border: 1px solid #888;"
        >
            <!-- Heading -->
            <div class="flex flex-col items-center gap-3">
                <i class="fa-solid fa-hurricane text-4xl text-[#fe832d]"></i>
                <h1 class="text-xl font-bold text-white">Forgot Password ?</h1>
            </div>

            <div class="flex flex-col gap-2 mt-4">
                <p class="text-sm text-[#e0e0e0]">
                    Seems like you forgot your password for <strong>Collabri</strong>. 
                    If this is true, we'll help you reset your password.
                </p>

                <div class="flex flex-col gap-2 mt-4">
                    <label for="email" class="text-sm font-semibold">Email :</label>
                    <div class="relative w-full">
                        <i class="pi pi-envelope absolute top-1/2 translate-y-[-50%] left-3"></i>
                        <input 
                            name="email"
                            type="email"
                            placeholder="user@example.com"
                            class="text-white py-2 pl-10 pr-2 rounded-lg w-full text-sm"
                            style="border: 1px solid #ddd;"
                            required="true"
                        />
                    </div>
                </div>

                <!-- Sign In Button -->
                <Button 
                    type="button" 
                    severity="contrast" 
                    label="Reset Password"
                    class="mt-2 text-sm"
                    />
            </div>
        </div>
    </div>
</template>