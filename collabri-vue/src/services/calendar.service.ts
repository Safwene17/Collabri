import axiosInstance from "../api/axios";

class CalendarService {
    constructor() {
        //
    }

    // Function to Create a Calendar
    async createCalendar(data: Object) {
        return axiosInstance.post("http://localhost:8222/api/v1/calendars", data, {
            headers: {
                "Content-Type": "application/json"
            }
        })
    };

    // Function to Update a Calendar
    async updateCalendar(data: any) {
        return axiosInstance.put(`http://localhost:8222/api/v1/calendars/${data.id}`, data, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };

    // Function to Delete a Calendar
    async deleteCalendar(calendarId: String | Number) {
        return axiosInstance.put(`http://localhost:8222/api/v1/calendars/${calendarId}`, {
            headers: {
                "Content-Type": "application/json"
            }
        });
    };
};