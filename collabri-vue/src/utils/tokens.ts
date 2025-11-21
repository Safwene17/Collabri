import axios from "axios";
/* 
    This file was created in purpose to separate the tokens'validation logic (email, password reset, etc...)
    from the Auth Service, as I thought creating an instance of the Auth Service into the routes file to use these
    functions isn't the best practice. So I've decided to create them in here.
*/

// Validate Email Verification Tokens
export async function validateTokens(url: string, token: string) {
    try {
        const validationResponse = await axios.post(url, {
            token: token
        }, {
            headers: {
                "Content-Type": "application/json"
            }
        });

        if(validationResponse.status === 200) {
            return true;
        } else {
            return false;
        }
    } catch(error) {
        console.error("Error when Validating Token:", error);

        return false;
    }
};