package com.friendavailability.domain.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException{
    public ResourceNotFoundException(String resourceName, Long id){
        super(
            String.format("%s with id %d not found", resourceName, id),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
        withDetail("resource", resourceName);
        withDetail("id", id);
    }

    public ResourceNotFoundException(String resourceName, String identifier){
        super(
            String.format("%s with id %s not found", resourceName, identifier),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
        withDetail("resource", resourceName);
        withDetail("identifier", identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}
