package com.discphy.ad.interfaces.api;

import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<?>> handle(CoreException e) {
        log.warn("CoreException : {}", e.getCustomMessage() != null ? e.getCustomMessage() : e.getMessage(), e);
        return failureResponse(e.getErrorType(), e.getCustomMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<?>> handle(MissingRequestHeaderException e) {
        return failureResponse(ErrorType.BAD_REQUEST, e.getHeaderName() + " 헤더가 누락되었습니다.");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<?>> handle(BindException e) {
        return failureResponse(ErrorType.BAD_REQUEST, e.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handle(NoResourceFoundException e) {
        return failureResponse(ErrorType.NOT_FOUND, null);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<?>> handle(Throwable e) {
        log.error("Exception : {}", e.getMessage(), e);
        return failureResponse(ErrorType.INTERNAL_ERROR, e.getMessage());
    }

    private ResponseEntity<ApiResponse<?>> failureResponse(ErrorType errorType, String errorMessage) {
        return new ResponseEntity<>(
            ApiResponse.fail(errorType.getCode(), errorMessage != null ? errorMessage : errorType.getMessage()),
            errorType.getStatus()
        );
    }
}
