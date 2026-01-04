package com.vty.vty.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@RequiredArgsConstructor
public class BusinessError {
    private final HttpStatus status;
    private final String error;
    private final String message;
    private final Long timestamp = System.currentTimeMillis();
}
