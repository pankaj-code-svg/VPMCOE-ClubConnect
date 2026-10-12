package com.vpmcoe.clubconnect.model;

public enum EventStatus {

    UPCOMING("Upcoming", "primary"),
    ONGOING("Ongoing", "warning"),
    COMPLETED("Completed", "secondary"),
    CANCELLED("Cancelled", "danger");

    private final String displayName;
    private final String badgeColor;

    EventStatus(String displayName, String badgeColor) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeColor() {
        return badgeColor;
    }
}