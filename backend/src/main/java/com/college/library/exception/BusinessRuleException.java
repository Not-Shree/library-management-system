package com.college.library.exception;

import org.springframework.http.HttpStatus;

/**
 * A library rule was broken, e.g. BORROW_LIMIT_EXCEEDED, BOOK_UNAVAILABLE,
 * INVALID_RETURN, INVALID_RENEWAL, INVALID_PAYMENT, UNPAID_FINES.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String error, String message) {
        super(HttpStatus.BAD_REQUEST, error, message);
    }

    public BusinessRuleException(HttpStatus status, String error, String message) {
        super(status, error, message);
    }
}
