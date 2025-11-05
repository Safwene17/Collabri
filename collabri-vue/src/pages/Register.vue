<script lang="ts" setup>
    defineOptions({
        name: "Register"
    });
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
            <div class="flex items-center justify-center w-full h-full">
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
                        />
                    </div>

                    <!-- Sign up Button -->
                    <Button type="submit" severity="contrast" label="Sign up" />

                    <!-- Sign in Link -->
                    <a href="#" class="text-sm text-center font-light">
                        Already a member ? 
                        <RouterLink to="/login" class="underline font-semibold">Sign in !</RouterLink>
                    </a>
                </div>
            </div>
        </div>
    </div>
</template>