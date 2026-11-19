package com.homeseek.dto;

import java.util.Map;

/** Uniform JSON error body. "errors" maps a field name to its message for validation failures. */
public record ErrorResponse(String message, Map<String, String> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }
}
