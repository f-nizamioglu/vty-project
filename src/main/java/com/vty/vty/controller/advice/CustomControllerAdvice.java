package com.vty.vty.controller.advice;

import com.vty.vty.model.BusinessError;
import com.vty.vty.model.BusinessException;
import com.vty.vty.model.ErrorDesc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CustomControllerAdvice {
    @ExceptionHandler(value = Exception.class)
    protected ResponseEntity<Object> handle(Exception e) {
        log.error("Exception encountered:/ {}", e.getMessage());
        e.printStackTrace();
        String message;
        HttpStatus httpStatus;
        if (e instanceof BusinessException){
            message = ((BusinessException) e).getErrorMessage();
            httpStatus = ((BusinessException) e).getHttpStatus();
        } else {
            message = ErrorDesc.GENERAL_ERROR.getErrorDescription();
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        BusinessError businessError = new BusinessError(httpStatus, e.getMessage(), message);
        return new ResponseEntity<>(businessError, new HttpHeaders(), businessError.getStatus());
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException e) {
        log.error("Exception encountered. Access denied:/ {}", e.getMessage());
        e.printStackTrace();

        String message = "Bu işlem için yetkiniz yoktur.";
        HttpStatus httpStatus = HttpStatus.FORBIDDEN;

        BusinessError businessError = new BusinessError(httpStatus, e.getMessage(), message);
        return new ResponseEntity<>(businessError, new HttpHeaders(), businessError.getStatus());
    }
}
