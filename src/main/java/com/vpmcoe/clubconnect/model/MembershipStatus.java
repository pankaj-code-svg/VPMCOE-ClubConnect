package com.vpmcoe.clubconnect.model;

public enum MembershipStatus {

    PENDING("Pending Review", "warning"),
    APPROVED("Active Member", "success"),
    REJECTED("Application Declined", "danger");

    private final String displayName;
    private final String badgeColor;

    MembershipStatus(String displayName, String badgeColor) {
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