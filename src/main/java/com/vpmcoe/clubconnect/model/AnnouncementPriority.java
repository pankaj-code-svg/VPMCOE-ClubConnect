package com.vpmcoe.clubconnect.model;

public enum AnnouncementPriority {
    LOW("Notice", "secondary"),
    NORMAL("Important", "primary"),
    URGENT("Urgent Notice", "danger");

    private final String displayName;
    private final String badgeColor;

    private AnnouncementPriority(String displayName, String badgeColor) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getBadgeColor() {
        return this.badgeColor;
    }
}