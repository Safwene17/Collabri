<script lang="ts" setup>
import { RouterLink, useRoute } from 'vue-router';
import { computed, onMounted, ref } from 'vue';
import axios from 'axios';

    defineOptions({
        name: "EmailVerified"
    });

    const route = useRoute();

    // Data
    const isVerifying = ref(false);

    // On Mounted
    onMounted(verifyEmail);

    // Email Verification Token
    // const token = computed(() => );

    // Function to Get Token & Use it to verify the email (if Token is valid)
    async function verifyEmail() {
        if(isVerifying.value) return;

        isVerifying.value = true;

        try {
            const token = route.query.token as string || "";

            if(!token) {
                console.log("zabb");
            }

            const verifyResponse = await axios.post("http://localhost:8222/api/v1/users/verify-email", {
                token: token
            }, {
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if(verifyResponse.status === 200) {
                console.log("EYYYYYYYYYYYYYYY");
            } else{
                console.log("AHHHHHHHHHHHHHHH");
            }
        } catch(error) {
            console.error("Error in Verify Email: ", error);

        } finally {
            isVerifying.value = false;
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
            <!-- Email Icon -->
            <div class="relative text-center">
                <i class="fa-solid fa-envelope text-8xl"></i>
                <i class="fa-solid fa-circle-check text-3xl absolute left-1/2 translate-x-[-50%] top-1/2 -translate-y-full text-green-500"></i>
            </div>

            <!-- Congrats Message & Paragraph -->
            <div class="flex flex-col items-center gap-3">
                <h1 class="text-3xl font-extralight">Congratulations !</h1>
                <p class="text-center font-extralight">
                    Your account is now verified, you can access it now and enjoy your experience on our app.
                </p>

                <RouterLink 
                    to="/login"
                    class="text-black bg-white rounded-md py-2 px-3 text-sm font-semibold mt-2"
                >
                    <i class="fa-solid fa-right-to-bracket mr-2"></i>
                    <span>Sign In</span>
                </RouterLink>
            </div>
        </div>
    </div>
</template>