package com.grace.platform.shared.infrastructure.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(int errorCode, String entityName, String id) {
        super(errorCode, entityName + " not found: " + id);
    }
}
