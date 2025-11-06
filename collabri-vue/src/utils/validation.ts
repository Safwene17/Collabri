import * as z from "zod";
import type { LoginValidationProps } from "../types/types";

// Login Schema
export const LoginSchema = z.object({
    email: z.string().email({ error: "Invalid Email" }).nonempty({ error: "Email cannot be empty" }),
    password: z.string().nonempty("Password cannot be empty")
});

// Function to Validate Login Input Data
export async function validateLoginInputs({ Player, userEmail, userPassword, toast }: LoginValidationProps) {
    try {
        await Player.parseAsync({ 
            email: userEmail, 
            password: userPassword
        });

        return true;
        
    } catch(error: any) {
        console.error("Error in Validate Inputs: ", error);

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