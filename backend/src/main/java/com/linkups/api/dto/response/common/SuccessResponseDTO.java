package com.linkups.api.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Success Response DTO
 *
 * Generic success response for operations that don't return specific data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponseDTO {

    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private boolean success = true;

    public static SuccessResponseDTO of(String message) {
        return SuccessResponseDTO.builder()
                .message(message)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}