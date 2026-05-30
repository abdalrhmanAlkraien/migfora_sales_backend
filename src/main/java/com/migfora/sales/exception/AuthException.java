package com.migfora.sales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:32 PM
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
