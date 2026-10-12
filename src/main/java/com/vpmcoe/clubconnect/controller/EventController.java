package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.MembershipService;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final ClubService clubService;
    private final MembershipService membershipService;

    public EventController(
            EventService eventService,
            ClubService clubService,
            MembershipService membershipService) {

        this.eventService = eventService;
        this.clubService = clubService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public String listEvents(
            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "clubId", required = false)
            Long clubId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            Model model) {

        List<Event> events;

        if (clubId != null) {

            clubService.findById(clubId)
                    .ifPresent(club ->
                            model.addAttribute("selectedClub", club)
                    );

            if (clubService.findById(clubId).isPresent()) {

                events = eventService.findByClub(
                        clubService.findById(clubId).get()
                );

            } else {

                events = eventService.findAllUpcoming();
            }

        } else if (search != null && !search.isBlank()) {

            events = eventService.searchEvents(search);

            model.addAttribute(
                    "searchQuery",
                    search
            );

        } else {

            events = eventService.findAllUpcoming();
        }

        User user = userDetails == null ? null : userDetails.getUser();
        model.addAttribute("events", events.stream().filter(event -> canView(event, user)).toList());

        model.addAttribute(
                "allClubs",
                clubService.findAllActive()
        );

        return "events/list";
    }

    @GetMapping("/{id}")
    public String eventDetail(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model) {

        Event event = eventService.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found with ID: " + id
                        )
                );

        if (!eventService.isActive(event) || !canView(event, userDetails == null ? null : userDetails.getUser())) {
            return "redirect:/events";
        }

        long registeredCount =
                eventService.countRegistrations(event);

        int remainingSeats = Math.max(
                0,
                event.getMaxCapacity() - (int) registeredCount
        );

        model.addAttribute("event", event);
        model.addAttribute(
                "registeredCount",
                registeredCount
        );
        model.addAttribute(
                "remainingSeats",
                remainingSeats
        );

        if (userDetails != null) {

            User currentUser =
                    userDetails.getUser();

            Optional<EventRegistration> registration =
                    eventService.getRegistration(
                            event,
                            currentUser
                    );

            model.addAttribute(
                    "userRegistration",
                    registration.orElse(null)
            );
        }

        return "events/detail";
    }

    private boolean canView(Event event, User user) {
        if (event.isVisibleToAll()) return true;
        if (user == null) return false;
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (membershipService.isApprovedMember(event.getClub(), user)) return true;
        return clubService.findByLead(user)
                .map(club -> club.getId().equals(event.getClub().getId()))
                .orElse(false);
    }
}
