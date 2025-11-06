import * as z from "zod";
import type { LoginValidationProps, RegisterValidationProps } from "../types/types";

// Login Schema
export const LoginSchema = z.object({
    email: z.string().trim().nonempty({ error: "Email cannot be empty" }).email({ error: "Invalid Email" }),
    password: z.string().trim().nonempty("Password cannot be empty")
});

// Function to Validate Login Input Data
export async function validateLoginInputs({ Schema, userEmail, userPassword, toast }: LoginValidationProps) {
    try {
        await Schema.parseAsync({ 
            email: userEmail, 
            password: userPassword
        });

        return true;
        
    } catch(error: any) {
        console.error("Error in Validate Login Inputs: ", error);

        if(error instanceof z.ZodError) {
            error.issues.forEach((issue) => {
                toast.add({
                    severity: "warn",
                    summary: "Warning",
                    detail: issue.message || "Verify your information before proceeding",
                });
            });
        }

        return false;
    }
};


// Register Schema
export const RegisterSchema = z.object({
    firstname: z.string()
            .trim()
            .nonempty("Firstname cannot be empty")
            .min(3, "Firstname cannot contains less than 3 characters")
            .max(300, "Firstname cannot contains more than 300 characters"),
    lastname: z.string()
            .trim()
            .nonempty("Lastname cannot be empty")
            .min(3, "Lastname cannot contains less than 3 characters")
            .max(300, "Lastname cannot contains more than 300 characters"),
    email: z.string().trim().nonempty({ error: "Email cannot be empty" }).email({ error: "Invalid Email" }),
    password: z.string()
            .trim()
            .nonempty("Password cannot be empty")
            .min(8, "Password must contain at least 8 characters")
            .max(300, "Password cannot contain more than 300 characters")
            .regex(/^(?=.*[0-9])(?=.*[!@#$%^&*(),.?":{}|<>]).*$/, 
                "Password must contain at least one number and one special character")
});

// Function to Validate Register Input Data
export async function validateRegisterInputs({ Schema, firstname, lastname, email, password, toast }: RegisterValidationProps) {
    try {
        await Schema.parseAsync({
            firstname: firstname,
            lastname: lastname,
            email: email,
            password: password
        });

        return true;

    } catch(error: any) {
        console.error("Error in Validate Register Inputs: ", error);

        if(error instanceof z.ZodError) {
            error.issues.forEach((issue) => {
                toast.add({
                    severity: "warn",
                    summary: "Warning",
                    detail: issue.message || "Verify your information before proceeding",
                });
            });
        }

        return false;
    }
};