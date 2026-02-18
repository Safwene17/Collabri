import { handleRTAndValidationErrors } from "../../utils/utils";

export async function safeApiCall(fn: Function, toast: any = null) {
    try {
        return await fn();

    } catch(error) {
        console.error(error);

        handleRTAndValidationErrors(error, toast);
    }
};