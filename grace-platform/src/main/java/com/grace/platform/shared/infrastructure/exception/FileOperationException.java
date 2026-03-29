package com.grace.platform.shared.infrastructure.exception;

import com.grace.platform.shared.ErrorCode;

public class FileOperationException extends InfrastructureException {
    public FileOperationException(String message) {
        super(ErrorCode.FILE_OPERATION_ERROR, message);
    }

    public FileOperationException(String message, Throwable cause) {
        super(ErrorCode.FILE_OPERATION_ERROR, message, cause);
    }
}