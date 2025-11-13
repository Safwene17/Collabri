import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import './style.css';
import 'primeicons/primeicons.css';
import App from './App.vue';
import router from './router/main.route';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';
import Toast from 'primevue/toast';
import ProgressSpinner from 'primevue/progressspinner';
import ToastService from 'primevue/toastservice';

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

const app = createApp(App);

// Pinia for State Management
app.use(pinia);

app.use(PrimeVue, {
    theme: {
        preset: Aura
    }
});

// Implementing Router
app.use(router);

app.use(ToastService);

// PrimeVue Components
app.component("InputText", InputText);
app.component("Button", Button);
app.component("Toast", Toast);
app.component("ProgressSpinner", ProgressSpinner);


app.mount("#app");