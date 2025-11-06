import axios from "axios";
import type { LoginProps } from "../types/types";

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
            }
        });

        // Success Response
        if(loginResponse.status === 200) {
            console.log(loginResponse.data);
            
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