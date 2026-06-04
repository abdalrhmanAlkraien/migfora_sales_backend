package com.migfora.sales.exception;

import lombok.Getter;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 1:42 PM
 */
@Getter
public class PasswordChangeRequiredException extends RuntimeException {

    private final String session;

    public PasswordChangeRequiredException(String session, String message) {
        super(message);
        this.session = session;
    }
}
