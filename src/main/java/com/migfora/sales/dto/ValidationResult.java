package com.migfora.sales.dto;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:19 PM
 */
public record ValidationResult(
        boolean isAllowed,
        boolean isSkipped,
        boolean isBlocked,
        boolean hasCdnWarning,
        String cdnProvider,
        String message
) {
    public static ValidationResult allowed() {
        return new ValidationResult(true, false, false, false, null, null);
    }

    public static ValidationResult allowedWithWarning(String cdn, String msg) {
        return new ValidationResult(true, false, false, true, cdn, msg);
    }

    public static ValidationResult skipped(String reason) {
        return new ValidationResult(false, true, false, false, null, reason);
    }

    public static ValidationResult blocked(String reason) {
        return new ValidationResult(false, false, true, false, null, reason);
    }
}