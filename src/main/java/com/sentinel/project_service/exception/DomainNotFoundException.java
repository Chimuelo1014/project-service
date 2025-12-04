package com.sentinel.project_service.exception;

class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}
