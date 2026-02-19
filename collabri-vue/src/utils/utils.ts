// Function to handle Error & Validation responses and display toasts based on error code
export function handleRTAndValidationErrors(error: any, toast?: any) { // RT refers to "Rate Limiting"
    if (error.response && error.response.status === 429) {
        toast.add({
            severity: 'warn',
            summary: 'Warning',
            detail: 'Many attempts. Try again after 1 minute',
            life: 4000
        });

        return 429;
        
    } else if(error.response && error.response.status === 422 || error.response && error.response.status === 400) {
        const allErrors = error.response?.data?.errors || error.response?.data?.message;
        
        if (allErrors && typeof allErrors === 'object' && Object.keys(allErrors).length > 0) {
            let toastIndex = 0;
            
            Object.keys(allErrors).forEach(fieldName => {
                const fieldErrors = allErrors[fieldName];
                
                if (Array.isArray(fieldErrors) && fieldErrors.length > 0) {
                    fieldErrors.forEach(errorMessage => {
                        if (errorMessage && typeof errorMessage === 'string' && errorMessage.trim() !== '') {
                            setTimeout(() => {
                                toast.add({
                                    severity: 'warn',
                                    summary: `Warning`,
                                    detail: errorMessage,
                                    life: 4000
                                });
                            }, toastIndex * 300);
                            toastIndex++;
                        }
                    });
                }
            });
            
            if (toastIndex === 0) {
                toast.add({
                    severity: 'warn',
                    summary: 'Validation Error',
                    detail: 'Please verify your information before proceeding',
                    life: 4000
                });
            } 
        } else if(typeof allErrors === "string") {
            toast.add({
                severity: 'warn',
                summary: 'Warning',
                detail: allErrors,
                life: 4000
            });
        }
    } else {
        toast.add({
            severity: 'error',
            summary: 'Error',
            detail: error.response?.data?.message || "An error occurred. Please try again later",
            life: 4000
        });
    }
};


// Function to Hide/Show Password
export function togglePassword(passwordInputId: string, toggleIconId: string) {
    const passwordInput = document.getElementById(passwordInputId) as HTMLInputElement | null;
    const toggleIcon = document.getElementById(toggleIconId) as HTMLElement | null;

    if (!passwordInput || !toggleIcon) {
        return;
    }

    if (passwordInput.type === "text") {
        passwordInput.type = "password";
        toggleIcon.classList.remove("pi-eye-slash");
        toggleIcon.classList.add("pi-eye");

    } else if (passwordInput.type === "password") {
        passwordInput.type = "text";
        toggleIcon.classList.remove("pi-eye");
        toggleIcon.classList.add("pi-eye-slash");
    }
};