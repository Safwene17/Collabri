import axios from "axios";

export class AuthService {
    firstname: string | undefined;
    lastname: string | undefined;
    email: string;
    password: string;

    constructor(email: string, password: string, firstname?: string, lastname?: string) {
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
};