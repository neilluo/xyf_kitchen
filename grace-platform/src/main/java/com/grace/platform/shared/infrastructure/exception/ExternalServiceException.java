package com.grace.platform.shared.infrastructure.exception;

public class ExternalServiceException extends InfrastructureException {
    public ExternalServiceException(int errorCode, String serviceName, String detail) {
        super(errorCode, serviceName + " service error: " + detail);
    }
}
