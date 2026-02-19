import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.store';
import axios from 'axios';
import { validateTokens } from '../utils/tokens';

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
        beforeEnter: async(to: any) => {
            const validateToken = await validateTokens(
                "http://localhost:8222/api/v1/auth/validate-email-verification-token",
                to.query.token
            );
            
            if(!validateToken) {
                return { name: "link-expired" };
            }
        }
    },
    {
        path: '/link-expired', 
        name: 'link-expired',
        component: () => import("../pages/LinkExpired.vue"),
    },
    {
        path: '/reset-password',
        name: 'reset-password',
        component: () => import("../pages/ResetPassword.vue"),
        meta: { requiresGuest: true },
        beforeEnter: async(to: any) => {
            const validateToken = await validateTokens(
                "http://localhost:8222/api/v1/auth/validate-password-reset-token",
                to.query.token
            );
            
            if(!validateToken) {
                return { name: "link-expired" };
            }
        }
    },
    {
        path: '/home',
        component: () => import("../components/layout/HomeLayout.vue"),
        meta: { requiresAuth: true },
        children: [
            // {
            //     path: '',
            //     name: 'home',
            //     component: () => import("../pages/Dashboard.vue"),
            // },
            // {
            //     path: 'profile',
            //     name: 'profile',
            //     component: () => import("../pages/Profile.vue"),
            // },
            {
                path: '',   // matches /home
                name: 'home',
                component: () => import("../components/MainPage.vue"),
            },
        ]
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
                "http://localhost:8222/api/v1/auth/refresh-token",
                {},
                { withCredentials: true }
            );
            
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