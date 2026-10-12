package com.vpmcoe.clubconnect.model;

public enum Role {
    ROLE_ADMIN,
    ROLE_CLUB_LEAD,
    ROLE_TECH_CLUB_LEAD,
    ROLE_CULT_CLUB_LEAD,
    ROLE_SPORTS_CLUB_LEAD,
    ROLE_RESEARCH_CLUB_LEAD,
    ROLE_LITERARY_CLUB_LEAD,
    ROLE_SOCIAL_CLUB_LEAD,
    ROLE_STUDENT;

    public boolean isClubLead() {
        return this == ROLE_CLUB_LEAD
                || this == ROLE_TECH_CLUB_LEAD
                || this == ROLE_CULT_CLUB_LEAD
                || this == ROLE_SPORTS_CLUB_LEAD
                || this == ROLE_RESEARCH_CLUB_LEAD
                || this == ROLE_LITERARY_CLUB_LEAD
                || this == ROLE_SOCIAL_CLUB_LEAD;
    }

    public static Role forClubCategory(ClubCategory category) {
        return switch (category) {
            case TECHNICAL -> ROLE_TECH_CLUB_LEAD;
            case CULTURAL -> ROLE_CULT_CLUB_LEAD;
            case SPORTS -> ROLE_SPORTS_CLUB_LEAD;
            case RESEARCH -> ROLE_RESEARCH_CLUB_LEAD;
            case LITERARY -> ROLE_LITERARY_CLUB_LEAD;
            case SOCIAL -> ROLE_SOCIAL_CLUB_LEAD;
        };
    }
}
