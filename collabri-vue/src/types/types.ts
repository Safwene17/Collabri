// Login Function Props
export interface LoginProps {
    email: string, 
    password: string, 
    url: string,
    toast: any
};

// Login Validation Props
export interface LoginValidationProps {
    Schema: any,
    userEmail: string,
    userPassword: string,
    toast: any
};

// Register Validation Props
export interface RegisterValidationProps {
    Schema: any,
    firstname: string,
    lastname: string,
    email: string,
    password: string,
    toast: any
};

// Register Function Props
export interface RegisterProps {
    firstname: string,
    lastname: string,
    email: string,
    password: string,
    url: string,
    toast: any
};