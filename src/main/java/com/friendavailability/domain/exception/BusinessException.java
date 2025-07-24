package com.friendavailability.domain.exception;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

public abstract class BusinessException extends RuntimeException{
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus){
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause){
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }

    public String getErrorCode(){
        return errorCode;
    }

    public HttpStatus getHttpStatus(){
        return httpStatus;
    }

    public Map<String, Object> getDetails(){
        return new HashMap<>(details);
    }

    public BusinessException withDetail(String key, Object value){
        this.details.put(key,value);
        return this;
    }

    public BusinessException withDetails(Map<String, Object> details){
        this.details.putAll(details);
        return this;
    }
}
