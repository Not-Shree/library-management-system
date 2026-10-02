package com.library.lms.exception;

/**
 * Thrown when an action violates a library business rule
 * (e.g. borrowing limit exceeded, copy already issued, fine cap exceeded).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
