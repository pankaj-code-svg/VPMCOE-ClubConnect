package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {
    void deleteByUser(User user);

    void deleteByAnnouncement(Announcement announcement);

    void deleteByEvent(Event event);

    boolean existsByUserAndEventAndNotificationTypeAndStatus(
            User user, Event event, NotificationType notificationType, NotificationStatus status);
    boolean existsByUserAndAnnouncementAndNotificationTypeAndStatus(
            User user, Announcement announcement, NotificationType notificationType, NotificationStatus status);
    List<EmailNotificationLog> findTop100ByOrderBySentAtDesc();
    long countByStatus(NotificationStatus status);
}
