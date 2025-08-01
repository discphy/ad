package com.discphy.ad.exception;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final ErrorType errorType;
    private final String customMessage;

    public CoreException(ErrorType errorType, String customMessage) {
        super(customMessage != null ? customMessage : errorType.getMessage());
        this.errorType = errorType;
        this.customMessage = customMessage;
    }

    public CoreException(ErrorType errorType) {
        this(errorType, null);
    }
}
