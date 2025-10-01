package com.linkups.api.dto.response.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private Integer status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String traceId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> validationErrors;

    private String suggestion;
    
}
