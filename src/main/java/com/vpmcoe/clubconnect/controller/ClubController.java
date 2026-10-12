package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubCategory;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.AnnouncementService;
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
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final EventService eventService;
    private final AnnouncementService announcementService;
    private final MembershipService membershipService;

    public ClubController(
            ClubService clubService,
            EventService eventService,
            AnnouncementService announcementService,
            MembershipService membershipService) {

        this.clubService = clubService;
        this.eventService = eventService;
        this.announcementService = announcementService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public String listClubs(
            @RequestParam(value = "category", required = false)
            String categoryStr,

            @RequestParam(value = "search", required = false)
            String search,

            Model model) {

        List<Club> clubs;

        if (categoryStr != null && !categoryStr.isBlank()) {

            try {
                ClubCategory category =
                        ClubCategory.valueOf(categoryStr.toUpperCase());

                clubs = clubService.findByCategory(category);

                model.addAttribute(
                        "selectedCategory",
                        categoryStr
                );

            } catch (IllegalArgumentException e) {

                clubs = clubService.findAllActive();
            }

        } else if (search != null && !search.isBlank()) {

            clubs = clubService.searchClubs(search);

            model.addAttribute(
                    "searchQuery",
                    search
            );

        } else {

            clubs = clubService.findAllActive();
        }

        model.addAttribute("clubs", clubs);

        model.addAttribute(
                "categories",
                ClubCategory.values()
        );

        return "clubs/list";
    }

    @GetMapping("/{id}")
    public String clubDetail(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model) {

        Club club = clubService.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Club not found with ID: " + id
                        )
                );

        List<Event> clubEvents = eventService.findByClub(club).stream()
                .filter(event -> event.isVisibleToAll() || canViewClubEvents(userDetails, club))
                .toList();

        List<Announcement> clubAnnouncements = List.of();

        long activeMemberCount =
                membershipService.countActiveMembers(club);

        model.addAttribute("club", club);
        model.addAttribute("clubEvents", clubEvents);
        model.addAttribute(
                "clubAnnouncements",
                clubAnnouncements
        );
        model.addAttribute(
                "activeMemberCount",
                activeMemberCount
        );

        if (userDetails != null) {

            User currentUser =
                    userDetails.getUser();

            Optional<ClubMembership> membership =
                    membershipService.getMembership(
                            club,
                            currentUser
                    );

            model.addAttribute(
                    "userMembership",
                    membership.orElse(null)
            );

            boolean isClubMember = membership.isPresent()
                    && membership.get().getStatus() == MembershipStatus.APPROVED;
            boolean isClubLead = clubService.findByLead(currentUser)
                    .map(leadClub -> java.util.Objects.equals(leadClub.getId(), club.getId()))
                    .orElse(false);
            if (currentUser.getRole() == Role.ROLE_ADMIN || isClubMember || isClubLead) {
                clubAnnouncements = announcementService.findByClub(club);
            }
        }

        model.addAttribute("clubAnnouncements", clubAnnouncements);

        return "clubs/detail";
    }

    private boolean canViewClubEvents(CustomUserDetails userDetails, Club club) {
        if (userDetails == null) return false;
        User user = userDetails.getUser();
        return user.getRole() == Role.ROLE_ADMIN
                || membershipService.isApprovedMember(club, user)
                || clubService.findByLead(user)
                .map(leadClub -> java.util.Objects.equals(leadClub.getId(), club.getId()))
                .orElse(false);
    }
}
