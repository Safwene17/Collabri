import * as z from "zod";

// Login Schema
export const LoginSchema = z.object({
    email: z.string("Email must be a string")
            .trim()
            .nonempty({ error: "Email cannot be empty" })
            .email({ error: "Invalid Email" }),
    password: z.string("Password must be a string").trim().nonempty("Password cannot be empty")
});

// Register Schema
export const RegisterSchema = z.object({
    firstname: z.string("Firstname must be a string")
            .trim()
            .nonempty("Firstname cannot be empty")
            .min(3, "Firstname cannot contains less than 3 characters")
            .max(300, "Firstname cannot contains more than 300 characters"),
    lastname: z.string("Lastname must be a string")
            .trim()
            .nonempty("Lastname cannot be empty")
            .min(3, "Lastname cannot contains less than 3 characters")
            .max(300, "Lastname cannot contains more than 300 characters"),
    email: z.string("Email must be a string").trim().nonempty({ error: "Email cannot be empty" }).email({ error: "Invalid Email" }),
    password: z.string("Password must be a string")
            .trim()
            .nonempty("Password cannot be empty")
            .min(8, "Password must contain at least 8 characters")
            .max(300, "Password cannot contain more than 300 characters")
            .regex(/^(?=.*[0-9])(?=.*[!@#$%^&*(),.?":{}|<>]).*$/, 
                "Password must contain at least one number and one special character")
});

// Forgot Password Schema
export const forgotPasswordSchema = z.object({
    email: z.string("Email must be a string")
            .trim()
            .nonempty({ error: "Email cannot be empty" })
            .email({ error: "Invalid Email" }),
});


// Generic validation function
export async function validateInputs<T extends z.ZodTypeAny>(
    schema: any,
    data: z.infer<T>,
    toast: any
): Promise<boolean> {
    try {
        await schema.parseAsync(data);

        return true;
    
    } catch(error: any) {
        console.error("Validation Error: ", error);

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