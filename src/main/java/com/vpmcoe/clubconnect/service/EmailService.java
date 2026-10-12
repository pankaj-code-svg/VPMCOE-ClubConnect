package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.*;
import com.vpmcoe.clubconnect.repository.EmailNotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private final JavaMailSender mailSender;
    private final EmailNotificationLogRepository notificationLogRepository;
    private final EventPassPdfService eventPassPdfService;
    private final boolean enabled;
    private final String from;
    private final String fromName;
    private final String provider;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmailService(JavaMailSender mailSender,
                        EmailNotificationLogRepository notificationLogRepository,
                        EventPassPdfService eventPassPdfService,
                        @Value("${clubconnect.mail.enabled:false}") boolean enabled,
                        @Value("${clubconnect.mail.from}") String from,
                        @Value("${clubconnect.mail.from-name:ClubConnect}") String fromName,
                        @Value("${clubconnect.mail.provider:smtp}") String provider,
                        @Value("${clubconnect.mail.api-key:}") String apiKey,
                        ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.notificationLogRepository = notificationLogRepository;
        this.eventPassPdfService = eventPassPdfService;
        this.enabled = enabled;
        this.from = from;
        this.fromName = fromName;
        this.provider = provider.trim().toLowerCase(Locale.ROOT);
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Async
    public void sendRegistrationSubmittedEmail(User user) {
        deliver(user, null, null, NotificationType.REGISTRATION_SUBMITTED,
                "Registration Received - ClubConnect",
                "Your registration has been successfully submitted and is waiting for administrator approval.", null, null);
    }

    @Async
    public void sendRegistrationApprovedEmail(User user) {
        deliver(user, null, null, NotificationType.REGISTRATION_APPROVED,
                "Your ClubConnect Account Has Been Approved",
                "Great news! Your ClubConnect registration has been approved. You can now log in using your registered email address and password.", null, null);
    }

    @Async
    public void sendRegistrationRejectedEmail(User user) {
        deliverWithoutAudit(user,
                "ClubConnect Registration Update",
                "Thank you for registering with ClubConnect. Unfortunately, your registration request was not approved. You may contact the administrator or submit a new registration if appropriate.");
    }

    @Async
    public void sendAnnouncementEmail(User user, Announcement announcement, NotificationType type) {
        if (alreadySent(user, null, announcement, type)) return;
        String scope = announcement.getClub() == null ? "ClubConnect" : announcement.getClub().getName();
        deliver(user, null, announcement, type, "New Announcement: " + announcement.getTitle(),
                "A new announcement has been posted by " + scope + ".<br><br><b>" + announcement.getTitle()
                        + "</b><br>Priority: " + announcement.getPriority().getDisplayName()
                        + "<br><br>" + announcement.getContent(), null, null);
    }

    @Async
    public void sendNewEventNotification(User user, Event event) {
        if (alreadySent(user, event, null, NotificationType.NEW_EVENT)) return;
        deliver(user, event, null, NotificationType.NEW_EVENT, "New Event: " + event.getTitle(),
                eventDetails(event) + "<br><br>Register through ClubConnect before the registration deadline.", null, null);
    }

    @Async
    public void sendEventRegistrationConfirmation(EventRegistration registration) {
        if (alreadySent(registration.getStudent(), registration.getEvent(), null,
                NotificationType.EVENT_REGISTRATION_CONFIRMED)) return;
        byte[] pass = eventPassPdfService.createPass(registration);
        deliver(registration.getStudent(), registration.getEvent(), null,
                NotificationType.EVENT_REGISTRATION_CONFIRMED,
                "Your Event Pass - " + registration.getEvent().getTitle(),
                "Your event registration is confirmed.<br><br>" + eventDetails(registration.getEvent())
                        + "<br><br>Registration ID: <b>" + registration.getTicketNumber()
                        + "</b><br><br>Your personalized Event Pass is attached.", pass,
                "ClubConnect-Event-Pass-" + registration.getTicketNumber() + ".pdf");
    }

    @Async
    public void sendEventCancelledEmail(User user, String eventTitle, String clubName,
                                        java.time.LocalDateTime eventDate) {
        String message = "The following event has been cancelled by the organizer. Your registration and event pass are no longer valid."
                + "<br><br><b>" + eventTitle + "</b>"
                + "<br>Organized by: " + clubName
                + "<br>Scheduled for: " + format(eventDate)
                + "<br><br>Please contact the club lead if you need more information.";
        deliverWithoutAudit(user, "Event Cancelled: " + eventTitle, message);
    }

    @Async
    public void sendEventReminder(User user, Event event, NotificationType type) {
        if (alreadySent(user, event, null, type)) return;
        String title = switch (type) {
            case REGISTRATION_DEADLINE_REMINDER -> "Registration Closing Soon: ";
            case EVENT_TOMORROW_REMINDER -> "Reminder: ";
            case EVENT_STARTING_SOON_REMINDER -> "Event Starting Soon: ";
            default -> "Event Update: ";
        };
        String message = type == NotificationType.REGISTRATION_DEADLINE_REMINDER
                ? "Registration closes on " + format(event.getRegistrationDeadline()) + ". Register soon.<br><br>" + eventDetails(event)
                : "You are registered for this upcoming event.<br><br>" + eventDetails(event) + "<br><br>Please keep your Event Pass ready.";
        deliver(user, event, null, type, title + event.getTitle(), message, null, null);
    }

    private boolean alreadySent(User user, Event event, Announcement announcement, NotificationType type) {
        if (event != null) {
            return notificationLogRepository.existsByUserAndEventAndNotificationTypeAndStatus(
                    user, event, type, NotificationStatus.SENT);
        }
        return announcement != null && notificationLogRepository
                .existsByUserAndAnnouncementAndNotificationTypeAndStatus(user, announcement, type, NotificationStatus.SENT);
    }

    @Transactional
    protected void deliver(User user, Event event, Announcement announcement, NotificationType type,
                           String subject, String message, byte[] attachment, String attachmentName) {
        if (!enabled || user.getEmail() == null || user.getEmail().isBlank()) return;
        try {
            sendEmail(user, subject, message, attachment, attachmentName);
            notificationLogRepository.save(new EmailNotificationLog(user, event, announcement, type,
                    NotificationStatus.SENT, null));
        } catch (Exception ex) {
            notificationLogRepository.save(new EmailNotificationLog(user, event, announcement, type,
                    NotificationStatus.FAILED, safeFailureReason(ex)));
        }
    }

    private void sendEmail(User user, String subject, String message,
                           byte[] attachment, String attachmentName) throws Exception {
        String body = html(user.getFullName(), message);
        if ("resend".equals(provider)) {
            sendWithResend(user.getEmail(), subject, body, attachment, attachmentName);
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, attachment != null,
                StandardCharsets.UTF_8.name());
        helper.setFrom(from, fromName);
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(body, true);
        if (attachment != null) {
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
        }
        mailSender.send(mimeMessage);
    }

    private void sendWithResend(String recipient, String subject, String body,
                                byte[] attachment, String attachmentName) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", formatFrom());
        payload.put("to", List.of(recipient));
        payload.put("subject", subject);
        payload.put("html", body);
        if (attachment != null) {
            payload.put("attachments", List.of(Map.of(
                    "filename", attachmentName,
                    "content", Base64.getEncoder().encodeToString(attachment)
            )));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Resend API returned HTTP " + response.statusCode() + ".");
        }
    }

    private String formatFrom() {
        if (from.contains("<")) return from;
        return fromName + " <" + from + ">";
    }

    private String safeFailureReason(Exception exception) {
        if (exception instanceof IOException && exception.getMessage() != null
                && exception.getMessage().startsWith("Resend API returned HTTP ")) {
            return exception.getMessage();
        }
        if (exception instanceof IllegalStateException && exception.getMessage() != null
                && exception.getMessage().equals("RESEND_API_KEY is not configured.")) {
            return exception.getMessage();
        }
        return "Delivery could not be completed.";
    }

    private String eventDetails(Event event) {
        return "<b>" + event.getTitle() + "</b><br>Organized by: " + event.getClub().getName()
                + "<br>Date and time: " + format(event.getEventDate())
                + "<br>Venue: " + event.getVenue() + "<br>Mode: " + event.getMode().name();
    }

    private void deliverWithoutAudit(User user, String subject, String message) {
        if (!enabled || user.getEmail() == null || user.getEmail().isBlank()) return;
        try {
            sendEmail(user, subject, message, null, null);
        } catch (Exception ignored) {
        }
    }

    private String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "To be announced" : dateTime.format(DATE_TIME);
    }

    private String html(String fullName, String message) {
        String name = fullName == null || fullName.isBlank() ? "" : " " + fullName;
        return "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;color:#212529'>"
                + "<div style='background:#0d6efd;color:white;padding:24px'><b>ClubConnect</b><br><small>VPM College of Engineering</small></div>"
                + "<div style='padding:28px;background:#f8f9fa'>Hello" + name + ",<br><br>" + message
                + "<br><br>Thank you,<br><b>ClubConnect</b><br>VPM College of Engineering</div>"
                + "<div style='padding:16px;text-align:center;font-size:12px;color:#6c757d'>© ClubConnect, VPM College of Engineering</div></div>";
    }
}
