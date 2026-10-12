package com.vpmcoe.clubconnect.model;

public enum ClubCategory {

    TECHNICAL("Technical & Coding", "bi-code-slash", "primary"),
    CULTURAL("Cultural & Arts", "bi-palette", "danger"),
    SPORTS("Sports & Fitness", "bi-trophy", "success"),
    RESEARCH("Research & Innovation", "bi-lightbulb", "info"),
    LITERARY("Literary & Debating", "bi-journal-bookmark", "warning"),
    SOCIAL("Social & Community", "bi-people", "secondary");

    private final String displayName;
    private final String iconClass;
    private final String badgeColor;

    ClubCategory(String displayName, String iconClass, String badgeColor) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconClass() {
        return iconClass;
    }

    public String getBadgeColor() {
        return badgeColor;
    }
}