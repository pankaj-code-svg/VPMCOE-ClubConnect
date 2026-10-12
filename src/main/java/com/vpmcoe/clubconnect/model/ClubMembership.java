package com.vpmcoe.clubconnect.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "club_memberships",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"club_id", "student_id"})
        }
)
public class ClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole clubRole;

    @Column(columnDefinition = "TEXT")
    private String applicationReason;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(nullable = true)
    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    public ClubMembership() {
        this.status = MembershipStatus.PENDING;
        this.clubRole = ClubRole.GENERAL_MEMBER;
        this.appliedAt = LocalDateTime.now();
    }

    public ClubMembership(
            Club club,
            User student,
            MembershipStatus status,
            ClubRole clubRole,
            String applicationReason,
            String skills
    ) {
        this.club = club;
        this.student = student;
        this.status = status;
        this.clubRole = clubRole;
        this.applicationReason = applicationReason;
        this.skills = skills;
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public ClubRole getClubRole() {
        return clubRole;
    }

    public void setClubRole(ClubRole clubRole) {
        this.clubRole = clubRole;
    }

    public String getApplicationReason() {
        return applicationReason;
    }

    public void setApplicationReason(String applicationReason) {
        this.applicationReason = applicationReason;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}