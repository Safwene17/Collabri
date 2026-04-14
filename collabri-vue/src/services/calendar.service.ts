import axiosInstance from "../api/axios";
import axios from "axios";

export class CalendarService {
    constructor() {
        //
    }

    // Function to Fetch all Calendars
    async getPublicCalendars() {
        return axios.get(`http://localhost:8222/api/v1/calendars`);
    };

    // Function to Create a Calendar
    async createCalendar(data: Object) {
        return axiosInstance.post(`/calendars`, data, {
            headers: {
                "Content-Type": "application/json"
            }
        })
    };

    // Function to Update a Calendar
    async updateCalendar(data: any) {
        return axiosInstance.put(`/calendars/${data.id}`, data, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Delete a Calendar
    async deleteCalendar(calendarId: String | Number) {
        return axiosInstance.delete(`/calendars/${calendarId}`, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Search for Calendars
    async getCalendarByID(calendarId: string | number) {
        return axiosInstance.get(`/calendars/${calendarId}`, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Search for Calendars
    async searchPublicCalendars(data: any) {
        return axiosInstance.get(`/calendars/search`, {
            data: data,
            headers: {
                "Content-Type": "application/json"
            }
        });
    };
};