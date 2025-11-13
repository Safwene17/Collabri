import axios from "axios";
import type { LoginProps, RegisterProps } from "../types/types";
import { handleRTAndValidationErrors } from "../utils/utils";
import { useAuthStore } from "../stores/auth";

// Login Function
export async function loginRequest({ email, password, url, toast }: LoginProps) {
    try {
        const loginResponse = await axios.post(url, {
                email: email,
                password: password,
            },
            {
            headers: {
                "Content-Type": "application/json",
            },
            withCredentials: true
        });

        // Success Response
        if(loginResponse.status === 200) {
            console.log(loginResponse.data);

            const authStore = useAuthStore();
            authStore.setAccessToken(loginResponse.data.access_token);

            toast.add({
                severity: "success",
                summary: "Success",
                detail: "Logged in successfully",
                life: 3000
            });
        }

    } catch(error) {
        console.error("Error in Login User: ", error);

        toast.add({
            severity: "error",
            summary: "Error",
            detail: "Une erreur s'est produite. Réessayer plus tard",
            life: 3000
        });
    }
};


// Register Function
export async function registerRequest({ firstname, lastname, email, password, url, toast }: RegisterProps) {
    try {
        const registerResponse = await axios.post(url, {
                firstname: firstname,
                lastname: lastname,
                email: email,
                password: password,
            },
            {
            headers: {
                "Content-Type": "application/json",
            },
        });

        // Success Response
        if(registerResponse.status === 201) {
            return 201;
        }
        
    } catch(error: any) {
        console.error("Error in Register User: ", error);

        handleRTAndValidationErrors(error, toast);
    }
};