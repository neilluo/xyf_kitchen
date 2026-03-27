package com.grace.platform.shared.infrastructure.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(int errorCode, String message) {
        super(errorCode, message);
    }
}
