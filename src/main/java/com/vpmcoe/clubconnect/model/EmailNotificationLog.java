package com.vpmcoe.clubconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_notification_logs")
public class EmailNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    private Announcement announcement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public EmailNotificationLog() { }

    public EmailNotificationLog(User user, Event event, Announcement announcement,
                                NotificationType notificationType, NotificationStatus status,
                                String errorMessage) {
        this.user = user;
        this.event = event;
        this.announcement = announcement;
        this.notificationType = notificationType;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Event getEvent() { return event; }
    public Announcement getAnnouncement() { return announcement; }
    public NotificationType getNotificationType() { return notificationType; }
    public NotificationStatus getStatus() { return status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getErrorMessage() { return errorMessage; }
}
