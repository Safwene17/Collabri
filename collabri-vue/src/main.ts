import { createApp } from 'vue';
import './style.css';
import 'primeicons/primeicons.css';
import App from './App.vue';
import router from './router/main';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';
import Toast from 'primevue/toast';
import ToastService from 'primevue/toastservice';

const app = createApp(App);

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


app.mount("#app");