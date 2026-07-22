package com.logshield.logshieldv2.model;

/**
 * Standard API response wrapper for all LogShield v2 endpoints.
 *
 * Every response follows this structure:
 * {
 *   "status": "success" or "error",
 *   "message": "human-readable description",
 *   "data": { ... actual payload ... }
 * }
 *
 * This gives the client a consistent contract regardless of which
 * endpoint they call. Adding new fields never breaks existing clients.
 *
 * Generic type T allows any payload type — LogEntryResponse,
 * List<LogEntryResponse>, statistics map, etc.
 */
public class ApiResponse<T> {

    private String status;   // "success" or "error"
    private String message;  // human-readable description
    private T      data;     // actual payload — null on error responses

    public ApiResponse() {}

    public ApiResponse(String status, String message, T data) {
        this.status  = status;
        this.message = message;
        this.data    = data;
    }

    // Static factory methods — cleaner than calling constructor directly
    // Caller writes: ApiResponse.success("Added", entry)
    // Not:           new ApiResponse<>("success", "Added", entry)

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null);
    }

    public String getStatus()  { return status; }
    public String getMessage() { return message; }
    public T      getData()    { return data; }

    public void setStatus(String status)   { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setData(T data)            { this.data = data; }
}