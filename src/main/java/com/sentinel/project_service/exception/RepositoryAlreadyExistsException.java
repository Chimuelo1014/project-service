package com.sentinel.project_service.exception;

public class RepositoryAlreadyExistsException extends RuntimeException {
    public RepositoryAlreadyExistsException(String message) {
        super(message);
    }
}