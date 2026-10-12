package com.vpmcoe.clubconnect.model;

public enum EventMode {

    OFFLINE("In-Person (Campus)"),
    ONLINE("Virtual / Online"),
    HYBRID("Hybrid (Campus & Online)");

    private final String displayName;

    EventMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}