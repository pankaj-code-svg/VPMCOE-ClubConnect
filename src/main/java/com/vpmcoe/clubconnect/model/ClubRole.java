package com.vpmcoe.clubconnect.model;

public enum ClubRole {

    LEAD("Club Lead / President"),
    CORE_MEMBER("Core Committee Member"),
    GENERAL_MEMBER("General Member");

    private final String displayName;

    ClubRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}