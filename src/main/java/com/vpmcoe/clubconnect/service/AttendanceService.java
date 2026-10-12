package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.AttendanceIncident;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.AttendanceIncidentRepository;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

@Service
@Transactional
public class AttendanceService {

    private final EventRegistrationRepository registrationRepository;
    private final AttendanceIncidentRepository incidentRepository;
    private final MembershipService membershipService;

    public AttendanceService(EventRegistrationRepository registrationRepository,
                             AttendanceIncidentRepository incidentRepository,
                             MembershipService membershipService) {
        this.registrationRepository = registrationRepository;
        this.incidentRepository = incidentRepository;
        this.membershipService = membershipService;
    }

    public ScanResult scan(Event event, Club scanningClub, User reporter, String rawTicketNumber) {
        String ticketNumber = rawTicketNumber == null ? "" : rawTicketNumber.trim().toUpperCase();
        Optional<EventRegistration> registrationOpt = ticketNumber.isBlank()
                ? Optional.empty()
                : registrationRepository.findByTicketNumber(ticketNumber);

        if (registrationOpt.isEmpty()) {
            recordIncident(event, scanningClub, reporter, null, ticketNumber,
                    "INVALID_TICKET", "No event pass matched the scanned code.");
            return new ScanResult(false, "Pass not recognized. An incident report was sent to the administrator.", null, true);
        }

        EventRegistration registration = registrationOpt.get();
        Event registeredEvent = registration.getEvent();
        if (!registeredEvent.getId().equals(event.getId())) {
            boolean crossClub = registeredEvent.getClub() != null
                    && scanningClub.getId() != null
                    && !registeredEvent.getClub().getId().equals(scanningClub.getId());
            String incidentType = crossClub ? "CROSS_CLUB_SCAN" : "WRONG_EVENT_PASS";
            String detail = registration.getStudent().getFullName()
                    + " (" + nullText(registration.getStudent().getPrn()) + ") presented a pass for "
                    + registeredEvent.getTitle() + " from "
                    + (registeredEvent.getClub() == null ? "an unassigned event" : registeredEvent.getClub().getName())
                    + " at the " + event.getTitle() + " scanner.";
            recordIncident(event, scanningClub, reporter, registration, ticketNumber, incidentType, detail);
            String message = crossClub
                    ? "This pass belongs to another club. Attendance was not recorded and an incident report was sent to the administrator."
                    : "This pass belongs to a different event. Attendance was not recorded and the incident was reported to the administrator.";
            return new ScanResult(false, message, registration, true);
        }

        if (!event.isVisibleToAll() && !membershipService.isApprovedMember(scanningClub, registration.getStudent())) {
            String detail = registration.getStudent().getFullName()
                    + " (" + nullText(registration.getStudent().getPrn())
                    + ") presented a pass for a club event without approved membership in "
                    + scanningClub.getName() + ".";
            recordIncident(event, scanningClub, reporter, registration, ticketNumber,
                    "CROSS_CLUB_SCAN", detail);
            return new ScanResult(false,
                    "The pass holder is not an approved member of this club. Attendance was not recorded and an incident report was sent to the administrator.",
                    registration, true);
        }

        boolean wasAlreadyPresent = registration.isAttended();
        if (!wasAlreadyPresent) {
            registration.setAttended(true);
            registration.setAttendedAt(LocalDateTime.now());
            registrationRepository.save(registration);
        }
        return new ScanResult(true,
                (wasAlreadyPresent ? "Already checked in: " : "Attendance recorded for ")
                        + registration.getStudent().getFullName() + ".",
                registration, false);
    }

    public EventRegistration setAttendance(EventRegistration registration, boolean attended) {
        registration.setAttended(attended);
        registration.setAttendedAt(attended ? LocalDateTime.now() : null);
        return registrationRepository.save(registration);
    }

    private void recordIncident(Event event, Club club, User reporter, EventRegistration registration,
                                String ticketNumber, String incidentType, String details) {
        AttendanceIncident incident = new AttendanceIncident();
        incident.setScanEvent(event);
        incident.setScanningClub(club);
        incident.setReporter(reporter);
        incident.setRegistration(registration);
        incident.setTicketNumber(ticketNumber.isBlank() ? "EMPTY_CODE" : ticketNumber);
        incident.setIncidentType(incidentType);
        incident.setDetails(details);
        incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> findAllAttendance() {
        return registrationRepository.findAll().stream()
                .sorted(Comparator.comparing((EventRegistration registration) -> registration.getEvent().getEventDate()).reversed()
                        .thenComparing(EventRegistration::getRegisteredAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventRegistration> findClubAttendance(Club club) {
        return registrationRepository.findAll().stream()
                .filter(registration -> registration.getEvent().getClub() != null
                        && registration.getEvent().getClub().getId().equals(club.getId()))
                .sorted(Comparator.comparing((EventRegistration registration) -> registration.getEvent().getEventDate()).reversed()
                        .thenComparing(EventRegistration::getRegisteredAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceIncident> findAllIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AttendanceIncident> findClubIncidents(Club club) {
        return incidentRepository.findByScanningClubOrderByCreatedAtDesc(club);
    }

    public Optional<AttendanceIncident> findIncident(Long id) {
        return incidentRepository.findById(id);
    }

    public void setIncidentResolved(Long id, boolean resolved) {
        AttendanceIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found."));
        incident.setResolved(resolved);
        incidentRepository.save(incident);
    }

    private String nullText(String value) {
        return value == null || value.isBlank() ? "no roll number" : value;
    }

    public record ScanResult(boolean successful, String message, EventRegistration registration, boolean incident) {}
}
