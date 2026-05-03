package com.apidoc.platform.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {

    private final HttpStatus status;

    /** Optional reference to {@code api_logs.id} when the failure was recorded. */
    private final Long logId;

    public DomainException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public DomainException(HttpStatus status, String message, Long logId) {
        super(message);
        this.status = status;
        this.logId = logId;
    }
}
