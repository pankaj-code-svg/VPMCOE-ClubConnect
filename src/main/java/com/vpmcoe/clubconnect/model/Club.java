package com.vpmcoe.clubconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Club name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubCategory category;

    @Column(nullable = false)
    private String shortTagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lead_user_id")
    private User lead;

    @Column(nullable = true)
    private String facultyAdvisor;

    @Column(nullable = true)
    private String roomVenue;

    @Column(nullable = true)
    private String meetingSchedule;

    @Column(nullable = true)
    private String contactEmail;

    @Column(nullable = true)
    private String bannerGradient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Club() {
        this.category = ClubCategory.TECHNICAL;
        this.status = ClubStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Club(
            String name,
            ClubCategory category,
            String shortTagline,
            String description,
            User lead,
            String facultyAdvisor,
            String roomVenue,
            String meetingSchedule,
            String contactEmail,
            String bannerGradient
    ) {
        this.name = name;
        this.category = category;
        this.shortTagline = shortTagline;
        this.description = description;
        this.lead = lead;
        this.facultyAdvisor = facultyAdvisor;
        this.roomVenue = roomVenue;
        this.meetingSchedule = meetingSchedule;
        this.contactEmail = contactEmail;
        this.bannerGradient = bannerGradient;
        this.status = ClubStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClubCategory getCategory() {
        return category;
    }

    public void setCategory(ClubCategory category) {
        this.category = category;
    }

    public String getShortTagline() {
        return shortTagline;
    }

    public void setShortTagline(String shortTagline) {
        this.shortTagline = shortTagline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getLead() {
        return lead;
    }

    public void setLead(User lead) {
        this.lead = lead;
    }

    public String getFacultyAdvisor() {
        return facultyAdvisor;
    }

    public void setFacultyAdvisor(String facultyAdvisor) {
        this.facultyAdvisor = facultyAdvisor;
    }

    public String getRoomVenue() {
        return roomVenue;
    }

    public void setRoomVenue(String roomVenue) {
        this.roomVenue = roomVenue;
    }

    public String getMeetingSchedule() {
        return meetingSchedule;
    }

    public void setMeetingSchedule(String meetingSchedule) {
        this.meetingSchedule = meetingSchedule;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getBannerGradient() {
        return bannerGradient;
    }

    public void setBannerGradient(String bannerGradient) {
        this.bannerGradient = bannerGradient;
    }

    public ClubStatus getStatus() {
        return status;
    }

    public void setStatus(ClubStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}