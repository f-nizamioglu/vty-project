package com.vty.vty.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class
BusinessException extends RuntimeException{

    private final HttpStatus httpStatus;
    private final String errorMessage;

    public static BusinessException generalError(){ return new BusinessException(HttpStatus.BAD_REQUEST, ErrorDesc.GENERAL_ERROR.getErrorDescription());}
    public static BusinessException notFound(){ return new BusinessException(HttpStatus.NOT_FOUND, ErrorDesc.GENERAL_NOT_FOUND_ERROR.getErrorDescription());}
    public static BusinessException unauthorized(){ return new BusinessException(HttpStatus.UNAUTHORIZED, ErrorDesc.UNAUTHORIZED.getErrorDescription());}
    public static BusinessException invalidOperation(){ return new BusinessException(HttpStatus.BAD_REQUEST, ErrorDesc.INVALID_OPERATION.getErrorDescription());}
    public static BusinessException passwordIncorrect(){ return new BusinessException(HttpStatus.BAD_REQUEST, ErrorDesc.PASSWORD_INCORRECT.getErrorDescription());}
}
