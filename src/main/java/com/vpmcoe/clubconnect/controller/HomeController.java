package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.AnnouncementService;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.MembershipService;
import com.vpmcoe.clubconnect.service.UserService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ClubService clubService;
    private final EventService eventService;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final MembershipService membershipService;

    public HomeController(
            ClubService clubService,
            EventService eventService,
            AnnouncementService announcementService,
            UserService userService,
            MembershipService membershipService) {

        this.clubService = clubService;
        this.eventService = eventService;
        this.announcementService = announcementService;
        this.userService = userService;
        this.membershipService = membershipService;
    }

    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Club> clubs = clubService.findAllActive();
        User currentUser = userDetails == null ? null : userService.findById(userDetails.getId()).orElse(userDetails.getUser());
        List<Event> upcomingEvents = eventService.findAllUpcoming().stream()
                .filter(event -> event.isVisibleToAll() || canViewClubNotice(currentUser, event.getClub()))
                .toList();
        List<Announcement> recentAnnouncements = announcementService.findAll().stream()
                .filter(announcement -> announcement.getClub() == null || canViewClubNotice(currentUser, announcement.getClub()))
                .limit(5)
                .toList();

        model.addAttribute("clubs", clubs);

        model.addAttribute(
                "upcomingEvents",
                upcomingEvents.stream().limit(4).toList()
        );

        model.addAttribute(
                "announcements",
                recentAnnouncements
        );

        model.addAttribute(
                "totalClubsCount",
                clubs.size()
        );

        model.addAttribute(
                "totalEventsCount",
                upcomingEvents.size()
        );

        model.addAttribute(
                "totalUsersCount",
                userService.countAll()
        );

        return "index";
    }

    private boolean canViewClubNotice(User user, Club club) {
        if (user == null) return false;
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        return membershipService.isApprovedMember(club, user)
                || clubService.findByLead(user).map(leadClub -> leadClub.getId().equals(club.getId())).orElse(false);
    }

    @GetMapping("/about")
    public String about(Model model) {

        model.addAttribute(
                "totalClubs",
                clubService.countAll()
        );

        return "about";
    }
}
