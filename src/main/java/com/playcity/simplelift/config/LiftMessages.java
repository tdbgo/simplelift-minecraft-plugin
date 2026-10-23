package com.playcity.simplelift.config;

public final class LiftMessages {
    private final java.util.Map<String, String> text;

    private final String destination;
    private final String currentFloor;
    private final String cantEnter;
    private final String cantLeave;
    private final String oneFloor;
    private final String unsafe;
    private final String floorNotExists;
    private final String scrollSelectEnabled;
    private final String scrollSelectDisabled;
    private final String scrollSelection;
    private final String scrollSelectConfirmed;
    private final String scrollSelectTimedOut;
    private final String scrollSelectBusy;
    private final String timeout;
    private final String busy;
    private final String noPermission;
    private final String internalError;

    public LiftMessages(String destination, String currentFloor, String cantEnter, String cantLeave,
            String oneFloor, String unsafe, String floorNotExists, String scrollSelectEnabled,
            String scrollSelectDisabled, String scrollSelection, String scrollSelectConfirmed,
            String scrollSelectTimedOut, String scrollSelectBusy, String timeout, String busy,
            String noPermission, String internalError, java.util.Map<String, String> text) {
        this.text = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(text));
        this.destination = destination;
        this.currentFloor = currentFloor;
        this.cantEnter = cantEnter;
        this.cantLeave = cantLeave;
        this.oneFloor = oneFloor;
        this.unsafe = unsafe;
        this.floorNotExists = floorNotExists;
        this.scrollSelectEnabled = scrollSelectEnabled;
        this.scrollSelectDisabled = scrollSelectDisabled;
        this.scrollSelection = scrollSelection;
        this.scrollSelectConfirmed = scrollSelectConfirmed;
        this.scrollSelectTimedOut = scrollSelectTimedOut;
        this.scrollSelectBusy = scrollSelectBusy;
        this.timeout = timeout;
        this.busy = busy;
        this.noPermission = noPermission;
        this.internalError = internalError;
    }

    public String destination() { return destination; }
    public String text(String key) { return text.get(key); }
    public String currentFloor() { return currentFloor; }
    public String cantEnter() { return cantEnter; }
    public String cantLeave() { return cantLeave; }
    public String oneFloor() { return oneFloor; }
    public String unsafe() { return unsafe; }
    public String floorNotExists() { return floorNotExists; }
    public String scrollSelectEnabled() { return scrollSelectEnabled; }
    public String scrollSelectDisabled() { return scrollSelectDisabled; }
    public String scrollSelection() { return scrollSelection; }
    public String scrollSelectConfirmed() { return scrollSelectConfirmed; }
    public String scrollSelectTimedOut() { return scrollSelectTimedOut; }
    public String scrollSelectBusy() { return scrollSelectBusy; }
    public String timeout() { return timeout; }
    public String busy() { return busy; }
    public String noPermission() { return noPermission; }
    public String internalError() { return internalError; }
}
