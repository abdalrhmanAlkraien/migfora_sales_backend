package com.migfora.sales.exception;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:03 PM
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
