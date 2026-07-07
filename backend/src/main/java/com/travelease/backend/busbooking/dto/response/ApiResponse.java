package com.travelease.backend.busbooking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "Application-level response message", example = "Request completed successfully")
    private String message;

    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "Validation or domain-specific errors")
    private Map<String, String> errors;

    @Schema(description = "Request path", example = "/api/buses")
    private String path;

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(int status, String message, T data, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .status(status)
                .data(data)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(int status, String message, Map<String, String> errors, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .status(status)
                .errors(errors)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
