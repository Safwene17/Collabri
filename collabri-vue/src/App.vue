<script lang="ts" setup>
import Login from './pages/Login.vue';
import Register from './pages/Register.vue';

  defineOptions({
    name: "App",
    components: {
      Login,
      Register,
    }
  });
</script>


<template>
  <RouterView />

  <Toast />
</template>