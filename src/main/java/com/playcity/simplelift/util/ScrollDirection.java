package com.playcity.simplelift.util;

public final class ScrollDirection {

    private ScrollDirection() {
    }

    public static boolean isForward(int newSlot, int previousSlot) {
        if (previousSlot == 0 && newSlot == 8) {
            return false;
        }
        if (previousSlot == 8 && newSlot == 0) {
            return true;
        }
        return previousSlot < newSlot;
    }
}
