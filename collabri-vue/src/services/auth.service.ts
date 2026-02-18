import axios from "axios";
import { useAuthStore } from "../stores/auth.store";
import axiosInstance from "../api/axios";

export class AuthService {
    firstname: string | undefined;
    lastname: string | undefined;
    email: string | undefined;
    password: string | undefined;

    constructor(email?: string, password?: string, firstname?: string, lastname?: string) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
    };

    // Register Request
    async register(url: string) {
        const registerResponse  = await axios.post(url, {
            firstname: this.firstname,
            lastname: this.lastname,
            email: this.email,
            password: this.password
        }, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        return registerResponse;
    };

    // Login Request
    async login(url: string) {
        const loginResponse = await axios.post(url, {
            email: this.email,
            password: this.password
        }, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        return loginResponse;
    };

    // Logout Request
    async logout(url: string) {
        try {
            const authStore = useAuthStore();

            const logoutResponse = await axiosInstance.post(url, {}, {
                headers: {
                    "Content-Type": "application/json"
                },
            });

            if(logoutResponse.status === 200) {
                authStore.clearAccessToken();
                window.location.href = "/login";
            }
        } catch (error: any) {
            console.error("Error in Logout:", error);
        }
    };

    // Verify Email Request
    async verifyEmail(url: string, token: string) {
        const verifyResponse = await axios.post(url, {
            token: token
        }, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        return verifyResponse;
    };

    // Reset Password Request
    async resetPassword(url: string, resetToken: string, newUserPassword: string) {
        const resetPasswordResponse = await axios.post(url, {
            newPassword: newUserPassword,
            token: resetToken
        }, {
            headers: {
                "Content-Type": "application/json"
            }   
        });

        return resetPasswordResponse;
    };
};