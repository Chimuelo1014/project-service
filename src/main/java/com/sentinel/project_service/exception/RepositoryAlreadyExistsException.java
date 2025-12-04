package com.sentinel.project_service.exception;

class RepositoryAlreadyExistsException extends RuntimeException {
    public RepositoryAlreadyExistsException(String message) {
        super(message);
    }
}
