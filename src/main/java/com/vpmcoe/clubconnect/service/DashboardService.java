package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.RegistrationStatus;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import com.vpmcoe.clubconnect.repository.ClubRepository;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import com.vpmcoe.clubconnect.repository.EventRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ClubMembershipRepository membershipRepository;

    public DashboardService(
            ClubRepository clubRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            ClubMembershipRepository membershipRepository
    ) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.membershipRepository = membershipRepository;
    }

    public Map<String, Object> getAdminDashboardStats() {

        Map<String, Object> stats = new HashMap<>();

        long totalClubs = clubRepository.count();

        long totalStudents =
                userRepository.countByRole(Role.ROLE_STUDENT);

        long totalEvents = eventRepository.findAll().stream()
                .filter(event -> event.getEventDate() != null)
                .filter(event -> event.getStatus() != com.vpmcoe.clubconnect.model.EventStatus.CANCELLED
                        && event.getStatus() != com.vpmcoe.clubconnect.model.EventStatus.COMPLETED)
                .filter(event -> !(event.getEndDate() == null
                        ? event.getEventDate()
                        : event.getEndDate()).isBefore(LocalDateTime.now()))
                .count();

        long totalRegistrations =
                registrationRepository.count();

        long pendingRegistrations =
                userRepository.countByRegistrationStatus(
                        RegistrationStatus.PENDING
                );

        stats.put("totalClubs", totalClubs);
        stats.put("totalStudents", totalStudents);
        stats.put("totalEvents", totalEvents);
        stats.put("totalRegistrations", totalRegistrations);
        stats.put("pendingRegistrations", pendingRegistrations);

        List<Club> allClubs =
                clubRepository.findAll();

        Map<String, Long> clubsByCategory =
                allClubs.stream()
                        .collect(Collectors.groupingBy(
                                club -> club.getCategory().getDisplayName(),
                                Collectors.counting()
                        ));

        stats.put(
                "categoryLabels",
                new ArrayList<>(clubsByCategory.keySet())
        );

        stats.put(
                "categoryCounts",
                new ArrayList<>(clubsByCategory.values())
        );

        List<EventRegistration> allRegistrations =
                registrationRepository.findAll();

        Map<String, Long> departmentCounts =
                allRegistrations.stream()
                        .filter(registration ->
                                registration.getStudent().getDepartment() != null
                                        && !registration.getStudent()
                                        .getDepartment()
                                        .isBlank()
                        )
                        .collect(Collectors.groupingBy(
                                registration ->
                                        registration.getStudent()
                                                .getDepartment(),
                                Collectors.counting()
                        ));

        stats.put(
                "deptLabels",
                new ArrayList<>(departmentCounts.keySet())
        );

        stats.put(
                "deptCounts",
                new ArrayList<>(departmentCounts.values())
        );

        return stats;
    }

    public Map<String, Object> getLeadDashboardStats(User lead) {

        Map<String, Object> stats = new HashMap<>();

        Optional<Club> clubOpt =
                clubRepository.findByLead(lead);

        if (clubOpt.isPresent()) {

            Club club = clubOpt.get();

            stats.put("club", club);

            long activeMembersCount =
                    membershipRepository.countByClubAndStatus(
                            club,
                            MembershipStatus.APPROVED
                    );

            stats.put(
                    "activeMembersCount",
                    activeMembersCount
            );

            long pendingRequestsCount =
                    membershipRepository.countByClubAndStatus(
                            club,
                            MembershipStatus.PENDING
                    );

            stats.put(
                    "pendingRequestsCount",
                    pendingRequestsCount
            );

            List<Event> clubEvents =
                    eventRepository
                            .findByClubOrderByEventDateDesc(club);

            LocalDateTime now = LocalDateTime.now();
            List<Event> liveEvents = clubEvents.stream()
                    .filter(event -> {
                        LocalDateTime expiry = event.getEndDate() != null
                                ? event.getEndDate()
                                : event.getEventDate();
                        return !expiry.isBefore(now)
                                && event.getStatus() != com.vpmcoe.clubconnect.model.EventStatus.CANCELLED
                                && event.getStatus() != com.vpmcoe.clubconnect.model.EventStatus.COMPLETED;
                    })
                    .collect(Collectors.toList());

            stats.put(
                    "eventsCount",
                    liveEvents.size()
            );

            long totalSignups =
                    liveEvents.stream()
                            .mapToLong(
                                    registrationRepository::countByEvent
                            )
                            .sum();

            stats.put(
                    "totalSignups",
                    totalSignups
            );

            List<ClubMembership> pendingRequests =
                    membershipRepository
                            .findByClubAndStatusOrderByAppliedAtDesc(
                                    club,
                                    MembershipStatus.PENDING
                            );

            stats.put(
                    "pendingRequests",
                    pendingRequests
            );

            stats.put(
                    "recentEvents",
                    liveEvents.stream()
                            .limit(5)
                            .collect(Collectors.toList())
            );

        } else {

            stats.put("club", null);

            stats.put(
                    "activeMembersCount",
                    0L
            );

            stats.put(
                    "pendingRequestsCount",
                    0L
            );

            stats.put(
                    "eventsCount",
                    0
            );

            stats.put(
                    "totalSignups",
                    0L
            );

            stats.put(
                    "pendingRequests",
                    Collections.emptyList()
            );

            stats.put(
                    "recentEvents",
                    Collections.emptyList()
            );
        }

        return stats;
    }

    public Map<String, Object> getStudentDashboardStats(
            User student
    ) {

        Map<String, Object> stats = new HashMap<>();

        List<ClubMembership> memberships =
                membershipRepository
                        .findByStudentOrderByAppliedAtDesc(
                                student
                        );

        List<EventRegistration> registrations =
                registrationRepository
                        .findByStudentOrderByRegisteredAtDesc(
                                student
                        );


        long approvedClubsCount =
                memberships.stream()
                        .filter(
                                membership ->
                                        membership.getStatus()
                                                == MembershipStatus.APPROVED
                        )
                        .count();


        stats.put(
                "approvedClubsCount",
                approvedClubsCount
        );

        stats.put(
                "totalRegistrationsCount",
                (long) registrations.size()
        );

        stats.put(
                "memberships",
                memberships
        );

        stats.put(
                "registrations",
                registrations
        );

        return stats;
    }
}
