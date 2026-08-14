package com.playcity.simplelift.config;

/** Raised when a configuration cannot be validated as a complete settings snapshot. */
public final class LiftConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LiftConfigurationException(String message) {
        super(message);
    }

    public LiftConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
