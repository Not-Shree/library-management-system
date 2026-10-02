package com.college.library.exception;

import org.springframework.http.HttpStatus;

/** e.g. DUPLICATE_ISBN, DUPLICATE_EMAIL, DUPLICATE_USERNAME. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String error, String message) {
        super(HttpStatus.CONFLICT, error, message);
    }
}
