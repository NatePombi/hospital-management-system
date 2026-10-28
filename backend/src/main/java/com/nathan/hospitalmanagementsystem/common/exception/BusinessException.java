package com.nathan.hospitalmanagementsystem.common.exception;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
    public BusinessException(String message,Throwable cause) {
        super(message,cause);
    }

}
