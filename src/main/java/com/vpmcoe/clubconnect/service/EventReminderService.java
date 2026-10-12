package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventStatus;
import com.vpmcoe.clubconnect.model.NotificationType;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import com.vpmcoe.clubconnect.repository.EventRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EventReminderService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public EventReminderService(EventRepository eventRepository,
                                EventRegistrationRepository registrationRepository,
                                UserRepository userRepository,
                                EmailService emailService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        eventRepository.findByRegistrationDeadlineBetweenAndStatus(
                        now.plusHours(23), now.plusHours(25), EventStatus.UPCOMING)
                .forEach(event -> userRepository.findAllApprovedAndEnabled().stream()
                        .filter(user -> !registrationRepository.existsByEventAndStudent(event, user))
                        .forEach(user -> emailService.sendEventReminder(user, event,
                                NotificationType.REGISTRATION_DEADLINE_REMINDER)));
        eventRepository.findByEventDateBetweenAndStatus(
                        now.plusHours(23), now.plusHours(25), EventStatus.UPCOMING)
                .forEach(event -> notifyAttendees(event, NotificationType.EVENT_TOMORROW_REMINDER));
        eventRepository.findByEventDateBetweenAndStatus(
                        now.plusMinutes(90), now.plusMinutes(150), EventStatus.UPCOMING)
                .forEach(event -> notifyAttendees(event, NotificationType.EVENT_STARTING_SOON_REMINDER));
    }

    private void notifyAttendees(Event event, NotificationType type) {
        registrationRepository.findByEvent(event).forEach(registration -> {
            User attendee = registration.getStudent();
            if (attendee.isEnabled() && attendee.isRegistrationApproved()) {
                emailService.sendEventReminder(attendee, event, type);
            }
        });
    }
}
