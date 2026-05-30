package com.migfora.sales.exception;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:33 PM
 */
public class TokenRefreshException extends RuntimeException {

    private final String token;

    public TokenRefreshException(String token, String message) {
        super(message);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
