import { createRouter, createWebHistory } from 'vue-router'
import Login from '../pages/Login.vue';
import Register from '../pages/Register.vue';
import ForgotPassword from '../pages/ForgotPassword.vue';


// All Routes
const routes = [
    { path: '/', redirect: '/login' }, // Default Current Path
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/forgot-password', component: ForgotPassword }
];


// Router Instance
const router = createRouter({
    history: createWebHistory(),
    routes,
});


export default router;