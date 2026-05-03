package com.apidoc.platform.infrastructure.persistence.entity;

/**
 * Distinguishes success vs error/failure response body trees stored on {@link ApiResponseField}.
 */
public enum ResponseKind {
    SUCCESS,
    FAILURE
}
