<script lang="ts" setup>
    defineOptions({
        name: "Login"
    });
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
                        <label for="email" class="font-semibold">Email :</label>
                        <InputText 
                            name="email" 
                            type="email" 
                            placeholder="user@example.com" 
                            class="text-black"
                        />
                    </div>

                    <!-- Password Input -->
                    <div class="flex flex-col gap-2">
                        <label for="email" class="font-semibold">Password :</label>
                        <InputText 
                            name="email" 
                            type="password" 
                            class="text-black"
                            placeholder="Your Password"
                        />
                    </div>

                    <!-- Sign In Button -->
                    <Button type="submit" severity="contrast" label="Sign in" />

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
            </div>
        </div>
    </div>
</template>