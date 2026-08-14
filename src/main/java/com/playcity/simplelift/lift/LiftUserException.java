package com.playcity.simplelift.lift;

/** A recoverable structure or usage problem that can be shown directly to a player. */
public final class LiftUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LiftUserException(String message) {
        super(message);
    }
}
