package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.EventStatus;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import com.vpmcoe.clubconnect.repository.EventRepository;
import com.vpmcoe.clubconnect.repository.EmailNotificationLogRepository;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final EmailNotificationLogRepository notificationLogRepository;
    private final ClubMembershipRepository membershipRepository;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository,
                        UserRepository userRepository,
                        EmailService emailService,
                        EmailNotificationLogRepository notificationLogRepository,
                        ClubMembershipRepository membershipRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationLogRepository = notificationLogRepository;
        this.membershipRepository = membershipRepository;
    }

    public Event saveEvent(Event event) {
        Event saved = eventRepository.save(event);
        if (saved.isVisibleToAll()) {
            userRepository.findAllApprovedAndEnabled().forEach(user ->
                    emailService.sendNewEventNotification(user, saved));
        } else if (saved.getClub() != null) {
            membershipRepository.findByClubAndStatusOrderByAppliedAtDesc(
                            saved.getClub(), MembershipStatus.APPROVED)
                    .stream()
                    .map(com.vpmcoe.clubconnect.model.ClubMembership::getStudent)
                    .filter(User::isEnabled)
                    .forEach(user -> emailService.sendNewEventNotification(user, saved));
        }
        return saved;
    }

    public Event saveEvent(Event event, MultipartFile poster) {
        applyPoster(event, poster);
        return saveEvent(event);
    }

    public Event updateEvent(Long id, Event updated) {

        Event existing = eventRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Event not found: " + id));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setEventDate(updated.getEventDate());
        existing.setEndDate(updated.getEndDate());
        existing.setVenue(updated.getVenue());
        existing.setMode(updated.getMode());
        existing.setOnlineLink(updated.getOnlineLink());
        existing.setRegistrationDeadline(updated.getRegistrationDeadline());
        existing.setMaxCapacity(updated.getMaxCapacity());
        existing.setStatus(updated.getStatus());

        if (updated.getBannerImage() != null
                && !updated.getBannerImage().isEmpty()) {

            existing.setBannerImage(updated.getBannerImage());
        }

        return eventRepository.save(existing);
    }

    public Event updateEvent(Long id, Event updated, MultipartFile poster) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setEventDate(updated.getEventDate());
        existing.setEndDate(updated.getEndDate());
        existing.setVenue(updated.getVenue());
        existing.setMode(updated.getMode());
        existing.setOnlineLink(updated.getOnlineLink());
        existing.setRegistrationDeadline(updated.getRegistrationDeadline());
        existing.setMaxCapacity(updated.getMaxCapacity());
        existing.setStatus(updated.getStatus());
        applyPoster(existing, poster);
        if (updated.getBannerImage() != null && !updated.getBannerImage().isEmpty()) {
            existing.setBannerImage(updated.getBannerImage());
        }
        return eventRepository.save(existing);
    }

    private void applyPoster(Event event, MultipartFile poster) {
        if (poster == null || poster.isEmpty()) return;
        String type = poster.getContentType();
        if (type == null || !List.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
            throw new IllegalArgumentException("Event poster must be JPG, PNG, WEBP, or GIF.");
        }
        if (poster.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Event poster must be 5 MB or smaller.");
        }
        try {
            event.setPosterImageData(poster.getBytes());
            event.setPosterImageContentType(type);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read the event poster.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Event> findAllUpcoming() {
        return eventRepository.findAll().stream()
                .filter(this::isActive)
                .sorted(java.util.Comparator.comparing(Event::getEventDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Event> findByClub(Club club) {
        return eventRepository.findByClubOrderByEventDateDesc(club).stream()
                .filter(this::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Event> findManagedByClub(Club club) {
        return eventRepository.findByClubOrderByEventDateDesc(club).stream()
                .filter(this::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isActive(Event event) {
        if (event.getEventDate() == null
                || event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.COMPLETED) {
            return false;
        }
        LocalDateTime expiry = event.getEndDate() == null
                ? event.getEventDate()
                : event.getEndDate();
        return !expiry.isBefore(LocalDateTime.now());
    }

    public void deleteByLead(Long eventId, Club leadClub) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        if (event.getClub() == null || !Objects.equals(event.getClub().getId(), leadClub.getId())) {
            throw new IllegalArgumentException("You can only delete events belonging to your club.");
        }
        registrationRepository.findByEvent(event).forEach(registration ->
                emailService.sendEventCancelledEmail(registration.getStudent(), event.getTitle(),
                        event.getClub().getName(), event.getEventDate()));
        notificationLogRepository.deleteByEvent(event);
        registrationRepository.deleteByEvent(event);
        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEvents(String keyword) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            return eventRepository.searchEvents(keyword.trim()).stream()
                    .filter(this::isActive)
                    .toList();
        }

        return findAllUpcoming();
    }

    public EventRegistration registerForEvent(Event event,
                                              User student) {

        if (!isActive(event)) {
            throw new IllegalStateException("This event is no longer available.");
        }

        if (registrationRepository.existsByEventAndStudent(event, student)) {
            throw new IllegalArgumentException(
                    "You are already registered for this event."
            );
        }

        long currentCount = registrationRepository.countByEvent(event);

        if (event.getMaxCapacity() > 0
                && currentCount >= event.getMaxCapacity()) {

            throw new IllegalStateException(
                    "Sorry, this event has reached maximum seating capacity ("
                            + event.getMaxCapacity() + ")."
            );
        }

        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now()
                .isAfter(event.getRegistrationDeadline())) {

            throw new IllegalStateException(
                    "Registration for this event has closed."
            );
        }

        String randomCode = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        String ticketNumber =
                "VPM-" + event.getId() + "-" + randomCode;

        EventRegistration registration =
                new EventRegistration(event, student, ticketNumber);

        EventRegistration savedRegistration = registrationRepository.save(registration);
        emailService.sendEventRegistrationConfirmation(savedRegistration);
        return savedRegistration;
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(Event event, User student) {
        return registrationRepository
                .existsByEventAndStudent(event, student);
    }

    @Transactional(readOnly = true)
    public Optional<EventRegistration> getRegistration(
            Event event,
            User student
    ) {
        return registrationRepository
                .findByEventAndStudent(event, student);
    }

    @Transactional(readOnly = true)
    public Optional<EventRegistration> getRegistrationById(
            Long registrationId
    ) {
        return registrationRepository.findById(registrationId);
    }

    @Transactional(readOnly = true)
    public Optional<EventRegistration> getRegistrationByTicket(
            String ticketNumber
    ) {
        return registrationRepository
                .findByTicketNumber(
                        ticketNumber.trim().toUpperCase()
                );
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> findUserRegistrations(
            User student
    ) {
        return registrationRepository
                .findByStudentOrderByRegisteredAtDesc(student);
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> findEventRegistrations(
            Event event
    ) {
        return registrationRepository
                .findByEventOrderByRegisteredAtDesc(event);
    }

    @Transactional(readOnly = true)
    public long countRegistrations(Event event) {
        return registrationRepository.countByEvent(event);
    }

    @Transactional(readOnly = true)
    public long countAttended(Event event) {
        return registrationRepository
                .countByEventAndAttendedTrue(event);
    }

    public boolean toggleAttendance(Long registrationId) {

        EventRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Registration not found: "
                                                + registrationId
                                ));

        boolean newStatus = !registration.isAttended();

        registration.setAttended(newStatus);

        registration.setAttendedAt(
                newStatus ? LocalDateTime.now() : null
        );

        registrationRepository.save(registration);

        return newStatus;
    }

    public EventRegistration submitFeedback(
            Long registrationId,
            int rating,
            String comment
    ) {

        EventRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Registration not found: "
                                                + registrationId
                                ));

        registration.setFeedbackRating(rating);
        registration.setFeedbackComment(comment);

        return registrationRepository.save(registration);
    }

    public void updateEventStatus(
            Long eventId,
            EventStatus newStatus
    ) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found: " + eventId
                        ));

        event.setStatus(newStatus);

        eventRepository.save(event);
    }
}
