package com.sentinel.project_service.exception;

class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}
