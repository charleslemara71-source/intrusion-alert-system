package com.zuvo.web;

/** Error that is sent to the browser as {"error": "message"} with an HTTP status. */
public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException bad(String m)       { return new ApiException(400, m); }
    public static ApiException unauthorized(String m) { return new ApiException(401, m); }
    public static ApiException forbidden(String m) { return new ApiException(403, m); }
    public static ApiException notFound(String m)  { return new ApiException(404, m); }
    public static ApiException conflict(String m)  { return new ApiException(409, m); }
    public static ApiException tooMany(String m)   { return new ApiException(429, m); }
}
