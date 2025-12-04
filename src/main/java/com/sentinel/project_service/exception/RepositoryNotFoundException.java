package com.sentinel.project_service.exception;

class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String message) {
        super(message);
    }
}
