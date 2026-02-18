<script lang="ts" setup>
import Header from '../Header.vue';
import Footer from '../Footer.vue';

    defineOptions({
        name: "Home"
    });
</script>

<!-- This component is just for testing protected routes -->
<template>
    <div class="bg-[#18181b] flex flex-col h-screen">
        <Header />
        
        <RouterView />

        <Footer />
    </div>
</template>