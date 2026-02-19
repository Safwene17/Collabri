import axiosInstance from "../api/axios";

const API_URL = import.meta.env.VITE_API_URL;


export class CalendarService {
    constructor() {
        //
    }

    // Function to Fetch all Calendars
    async getPublicCalendars() {
        return axiosInstance.get(`${API_URL}/calendars`, {
            headers: {
                "Content-Type": "application/json",
            },
            withCredentials: true,
        });
    };

    // Function to Create a Calendar
    async createCalendar(data: Object) {
        return axiosInstance.post(`${API_URL}/calendars`, data, {
            headers: {
                "Content-Type": "application/json"
            }
        })
    };

    // Function to Update a Calendar
    async updateCalendar(data: any) {
        return axiosInstance.put(`${API_URL}/calendars/${data.id}`, data, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Delete a Calendar
    async deleteCalendar(calendarId: String | Number) {
        return axiosInstance.delete(`${API_URL}/calendars/${calendarId}`, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Search for Calendars
    async getCalendarByID(calendarId: string | number) {
        return axiosInstance.get(`${API_URL}/calendars/${calendarId}`, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Search for Calendars
    async searchPublicCalendars(data: any) {
        return axiosInstance.get(`${API_URL}/calendars/search`, {
            data: data,
            headers: {
                "Content-Type": "application/json"
            }
        });
    };
};