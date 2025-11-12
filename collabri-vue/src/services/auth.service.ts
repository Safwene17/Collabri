import axios from "axios";

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
    }
};