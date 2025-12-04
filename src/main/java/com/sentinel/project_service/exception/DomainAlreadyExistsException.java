package com.sentinel.project_service.exception;

class DomainAlreadyExistsException extends RuntimeException {
    public DomainAlreadyExistsException(String message) {
        super(message);
    }
}
