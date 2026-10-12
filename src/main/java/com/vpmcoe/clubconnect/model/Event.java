package com.vpmcoe.clubconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column
    private Boolean visibleToAll = false;

    @NotNull(message = "Event start date & time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(nullable = false)
    private LocalDateTime eventDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(nullable = true)
    private LocalDateTime endDate;

    @NotBlank(message = "Venue or location is required")
    @Column(nullable = false)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventMode mode;

    @Column(nullable = true)
    private String onlineLink;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(nullable = true)
    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = true)
    private String bannerImage;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] posterImageData;

    private String posterImageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Event() {
        this.mode = EventMode.OFFLINE;
        this.maxCapacity = 100;
        this.status = EventStatus.UPCOMING;
        this.createdAt = LocalDateTime.now();
    }

    public Event(
            String title,
            String description,
            Club club,
            LocalDateTime eventDate,
            LocalDateTime endDate,
            String venue,
            EventMode mode,
            String onlineLink,
            LocalDateTime registrationDeadline,
            int maxCapacity,
            String bannerImage
    ) {
        this.title = title;
        this.description = description;
        this.club = club;
        this.eventDate = eventDate;
        this.endDate = endDate;
        this.venue = venue;
        this.mode = mode;
        this.onlineLink = onlineLink;
        this.registrationDeadline = registrationDeadline;
        this.maxCapacity = maxCapacity;
        this.bannerImage = bannerImage;
        this.status = EventStatus.UPCOMING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public boolean isVisibleToAll() {
        return Boolean.TRUE.equals(visibleToAll);
    }

    public void setVisibleToAll(boolean visibleToAll) {
        this.visibleToAll = visibleToAll;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public EventMode getMode() {
        return mode;
    }

    public void setMode(EventMode mode) {
        this.mode = mode;
    }

    public String getOnlineLink() {
        return onlineLink;
    }

    public void setOnlineLink(String onlineLink) {
        this.onlineLink = onlineLink;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public byte[] getPosterImageData() { return posterImageData; }
    public void setPosterImageData(byte[] posterImageData) { this.posterImageData = posterImageData; }
    public String getPosterImageContentType() { return posterImageContentType; }
    public void setPosterImageContentType(String posterImageContentType) { this.posterImageContentType = posterImageContentType; }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
