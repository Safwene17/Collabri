import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth';
import axios from 'axios';

// All Routes
const routes = [
    { path: '/', redirect: '/login' },
    {
        path: '/login',
        name: 'login',
        component: () => import("../pages/Login.vue"),
        meta: { requiresGuest: true }
    },
    {
        path: '/register',
        name: 'register', 
        component: () => import("../pages/Register.vue"),
        meta: { requiresGuest: true }
    },
    {
        path: '/forgot-password',
        name: 'forgot-password',
        component: () => import("../pages/ForgotPassword.vue"),
        meta: { requiresGuest: true }
    },
    {
        path: '/verify-email', 
        name: 'verify-email',
        component: () => import("../pages/EmailVerified.vue"),
        beforeEnter: (to: any) => {
            if (!to.query.token) {
                return { name: "login" };
            }
        }
    },
    {
        path: '/reset-password',
        name: 'reset-password',
        component: () => import("../pages/ResetPassword.vue"),
        meta: { requiresGuest: true },
        beforeEnter: (to: any) => {
            if (!to.query.token) {
                return { name: "login" };
            }
        }
    },
    {
        path: '/home',
        name: 'home',
        component: () => import("../pages/Home.vue"),
        meta: { requiresAuth: true }
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to, from) => {
    const authStore = useAuthStore();
    
    // If route requires auth and we don't have a token
    if(to.meta.requiresAuth && !authStore.accessToken) {
        try {
            const response = await axios.post(
                "http://localhost:8222/api/v1/users/refresh",
                {},
                { withCredentials: true }
            );
            
            // console.log(response);
            authStore.setAccessToken(response.data.accessToken);
            
            return true;
        } catch (error) {
            return { name: "login" };
        }
    }
    
    if(to.meta.requiresGuest && authStore.isAuthenticated) {
        return { name: "home" };
    }
    
    return true;
});

export default router;