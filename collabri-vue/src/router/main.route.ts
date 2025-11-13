import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth';

// All Routes
const routes = [
    { path: '/', redirect: '/login' }, // Default Current Path
    { 
        path: '/login', 
        name: 'login',
        component: () => import("../pages/Login.vue") 
    },
    { 
        path: '/register', 
        name: 'register',
        component: () => import("../pages/Register.vue") 
    },
    { 
        path: '/forgot-password', 
        name: 'forgot-password',
        component: () => import("../pages/ForgotPassword.vue") 
    },
    { 
        path: '/verify-email', 
        name: 'verify-email',
        component: () => import("../pages/EmailVerified.vue") 
    },
    { 
        path: '/home', 
        name: 'home',
        component: () => import("../pages/Home.vue"),
        meta: { requiresAuth: true }
    },
];


// Router Instance
const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore();
    
    if (to.meta.requiresAuth && !authStore.accessToken) {
        next({ name: "login" });
    } else {
        next();
    }
});


export default router;