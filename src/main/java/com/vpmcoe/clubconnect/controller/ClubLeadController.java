package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.AnnouncementPriority;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.ClubRole;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventMode;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.EventStatus;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.AnnouncementService;
import com.vpmcoe.clubconnect.service.AttendanceService;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.DashboardService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.MembershipService;
import com.vpmcoe.clubconnect.service.UserService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lead")
public class ClubLeadController {

    private final DashboardService dashboardService;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final AttendanceService attendanceService;

    public ClubLeadController(
            DashboardService dashboardService,
            ClubService clubService,
            MembershipService membershipService,
            EventService eventService,
            AnnouncementService announcementService,
            UserService userService,
            AttendanceService attendanceService
    ) {
        this.dashboardService = dashboardService;
        this.clubService = clubService;
        this.membershipService = membershipService;
        this.eventService = eventService;
        this.announcementService = announcementService;
        this.userService = userService;
        this.attendanceService = attendanceService;
    }

    private Club getLeadClub(
            CustomUserDetails userDetails
    ) {

        User user = userService
                .findById(userDetails.getId())
                .orElse(userDetails.getUser());

        return clubService
                .findByLead(user)
                .orElse(null);
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            Model model
    ) {

        User user = userService
                .findById(userDetails.getId())
                .orElse(userDetails.getUser());

        Map<String, Object> stats =
                dashboardService.getLeadDashboardStats(user);

        model.addAllAttributes(stats);
        model.addAttribute("currentUser", user);
        if (stats.get("club") instanceof Club club) {
            model.addAttribute("clubAnnouncements", announcementService.findByClub(club));
        }

        return "lead/dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            Model model
    ) {

        User user = userService
                .findById(userDetails.getId())
                .orElse(userDetails.getUser());

        model.addAttribute("user", user);

        return "lead/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam("fullName")
            String fullName,

            @RequestParam(
                    value = "phone",
                    required = false
            )
            String phone,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes
    ) {

        try {

            userService.updateBasicProfile(
                    userDetails.getId(),
                    fullName,
                    phone
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Profile updated successfully!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating profile: "
                            + e.getMessage()
            );
        }

        return "redirect:/lead/profile";
    }

    @PostMapping("/profile/account")
    public String updateAccount(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam("email") String email,
                                @RequestParam("currentPassword") String currentPassword,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "confirmNewPassword", required = false) String confirmNewPassword,
                                RedirectAttributes redirectAttributes,
                                jakarta.servlet.http.HttpServletRequest request) {
        try {
            boolean credentialsChanged = !email.trim().equalsIgnoreCase(userDetails.getUsername()) || (newPassword != null && !newPassword.isBlank());
            userService.updateAccountCredentials(userDetails.getId(), email, currentPassword, newPassword, confirmNewPassword);
            if (credentialsChanged) {
                request.getSession().invalidate();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                return "redirect:/login?accountUpdated=true";
            }
            redirectAttributes.addFlashAttribute("successMessage", "No account changes were needed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating account: " + e.getMessage());
        }
        return "redirect:/lead/profile";
    }

    @GetMapping("/members")
    public String members(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {

            model.addAttribute(
                    "errorMessage",
                    "You are not currently assigned as a lead to any active club."
            );

            return "lead/no-club";
        }

        List<ClubMembership> pendingList =
                membershipService.findPendingMemberships(club);

        List<ClubMembership> activeList =
                membershipService.findActiveMembers(club);

        model.addAttribute("club", club);
        model.addAttribute("pendingList", pendingList);
        model.addAttribute("activeList", activeList);

        return "lead/members";
    }

    @PostMapping("/members/{id}/approve")
    public String approveMembership(
            @PathVariable Long id,

            @RequestParam(
                    value = "clubRole",
                    defaultValue = "GENERAL_MEMBER"
            )
            ClubRole clubRole,

            @RequestParam(
                    value = "reviewNotes",
                    required = false
            )
            String reviewNotes,

            RedirectAttributes redirectAttributes
    ) {

        try {

            membershipService.approveMembership(
                    id,
                    clubRole,
                    reviewNotes
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Membership application approved successfully!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error approving membership: "
                            + e.getMessage()
            );
        }

        return "redirect:/lead/members";
    }

    @PostMapping("/members/{id}/reject")
    public String rejectMembership(
            @PathVariable Long id,

            @RequestParam(
                    value = "reviewNotes",
                    required = false
            )
            String reviewNotes,

            RedirectAttributes redirectAttributes
    ) {

        try {

            membershipService.rejectMembership(
                    id,
                    reviewNotes
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Application declined."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error declining application: "
                            + e.getMessage()
            );
        }

        return "redirect:/lead/members";
    }

    @GetMapping("/events")
    public String events(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        List<Event> events =
                eventService.findManagedByClub(club);

        model.addAttribute("club", club);
        model.addAttribute("events", events);

        return "lead/events";
    }

    @GetMapping("/events/new")
    public String newEventForm(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        Event event = new Event();

        event.setClub(club);

        model.addAttribute("event", event);
        model.addAttribute("club", club);
        model.addAttribute("modes", EventMode.values());

        return "lead/event-form";
    }

    @PostMapping("/events/new")
    public String createEvent(
            @ModelAttribute("event")
            @Valid
            Event event,

            BindingResult bindingResult,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @RequestParam(value = "poster", required = false) org.springframework.web.multipart.MultipartFile poster,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        if (bindingResult.hasErrors()) {

            model.addAttribute("club", club);
            model.addAttribute(
                    "modes",
                    EventMode.values()
            );

            return "lead/event-form";
        }

        event.setClub(club);

        try {
            eventService.saveEvent(event, poster);
        } catch (IllegalArgumentException e) {
            model.addAttribute("club", club);
            model.addAttribute("modes", EventMode.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "lead/event-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Event '" + event.getTitle()
                        + "' published successfully!"
        );

        return "redirect:/lead/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Club club = getLeadClub(userDetails);
        if (club == null) {
            return "lead/no-club";
        }
        try {
            eventService.deleteByLead(id, club);
            redirectAttributes.addFlashAttribute("successMessage", "Event and its registrations were deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lead/events";
    }

    @GetMapping("/events/{id}/edit")
    public String editEventForm(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        Event event = eventService
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found"
                        )
                );

        model.addAttribute("event", event);
        model.addAttribute("club", club);
        model.addAttribute(
                "modes",
                EventMode.values()
        );
        model.addAttribute(
                "statuses",
                EventStatus.values()
        );

        return "lead/event-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEvent(
            @PathVariable Long id,

            @ModelAttribute("event")
            @Valid
            Event event,

            BindingResult bindingResult,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @RequestParam(value = "poster", required = false) org.springframework.web.multipart.MultipartFile poster,

            Model model,

            RedirectAttributes redirectAttributes
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        if (bindingResult.hasErrors()) {

            model.addAttribute("club", club);
            model.addAttribute(
                    "modes",
                    EventMode.values()
            );
            model.addAttribute(
                    "statuses",
                    EventStatus.values()
            );

            return "lead/event-form";
        }

        try {
            Event existing = eventService.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found."));
            if (existing.getClub() == null || !java.util.Objects.equals(existing.getClub().getId(), club.getId())) {
                throw new IllegalArgumentException("You can only update events belonging to your club.");
            }
            eventService.updateEvent(id, event, poster);
        } catch (IllegalArgumentException e) {
            model.addAttribute("club", club);
            model.addAttribute("modes", EventMode.values());
            model.addAttribute("statuses", EventStatus.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "lead/event-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Event updated successfully!"
        );

        return "redirect:/lead/events";
    }

    @GetMapping("/events/{id}/attendees")
    public String eventAttendees(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        Event event = eventService
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found"
                        )
                );

        if (event.getClub() == null || !event.getClub().getId().equals(club.getId())) {
            return "redirect:/lead/events";
        }

        List<EventRegistration> registrations =
                eventService.findEventRegistrations(event);

        long attendedCount =
                eventService.countAttended(event);

        model.addAttribute("event", event);
        model.addAttribute(
                "registrations",
                registrations
        );
        model.addAttribute(
                "attendedCount",
                attendedCount
        );

        return "lead/attendees";
    }

    @GetMapping("/events/{id}/scanner")
    public String eventScanner(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        Club club = getLeadClub(userDetails);
        if (club == null) return "lead/no-club";
        Event event = eventService.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found."));
        if (event.getClub() == null || !event.getClub().getId().equals(club.getId()) || !eventService.isActive(event)) return "redirect:/lead/events";
        model.addAttribute("event", event);
        return "lead/event-scanner";
    }

    @PostMapping("/events/{id}/scan")
    @ResponseBody
    public Map<String, Object> scanEventPass(@PathVariable Long id,
                                             @RequestParam("ticketNumber") String ticketNumber,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Club club = getLeadClub(userDetails);
        if (club == null) return Map.of("success", false, "message", "No club is assigned to this account.");
        Event event = eventService.findById(id).orElse(null);
        if (event == null || event.getClub() == null || !event.getClub().getId().equals(club.getId())
                || !eventService.isActive(event)) {
            return Map.of("success", false, "message", "You cannot scan passes for this club event.");
        }
        AttendanceService.ScanResult result = attendanceService.scan(event, club, userDetails.getUser(), ticketNumber);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("success", result.successful());
        response.put("incident", result.incident());
        response.put("message", result.message());
        if (result.registration() != null) {
            EventRegistration registration = result.registration();
            User student = registration.getStudent();
            response.put("name", student.getFullName());
            response.put("prn", student.getPrn());
            response.put("department", student.getDepartment());
            response.put("academicYear", student.getAcademicYear());
            response.put("club", registration.getEvent().getClub().getName());
            response.put("event", registration.getEvent().getTitle());
            response.put("attendedAt", registration.getAttendedAt() == null ? "" : registration.getAttendedAt().toString());
        }
        return response;
    }

    @GetMapping("/attendance")
    public String attendanceRecords(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Club club = getLeadClub(userDetails);
        if (club == null) return "lead/no-club";
        model.addAttribute("club", club);
        model.addAttribute("records", attendanceService.findClubAttendance(club));
        model.addAttribute("incidents", attendanceService.findClubIncidents(club));
        return "lead/attendance";
    }

    @PostMapping("/events/{eventId}/attendees/{regId}/toggle")
    public String toggleAttendance(
            @PathVariable Long eventId,

            @PathVariable Long regId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes
    ) {

        try {

            Club club = getLeadClub(userDetails);
            Event event = eventService.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found."));
            EventRegistration registration = eventService.getRegistrationById(regId)
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found."));
            if (club == null || event.getClub() == null || !event.getClub().getId().equals(club.getId())
                    || !registration.getEvent().getId().equals(eventId)) {
                throw new IllegalArgumentException("You can only update attendance for your own club's event.");
            }
            if (!event.isVisibleToAll()
                    && !membershipService.isApprovedMember(club, registration.getStudent())) {
                throw new IllegalArgumentException("The attendee is not an approved member of this club.");
            }

            boolean attended = !registration.isAttended();
            attendanceService.setAttendance(registration, attended);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Attendance marked as "
                            + (attended
                            ? "Attended (Present)"
                            : "Absent")
                            + "."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating attendance: "
                            + e.getMessage()
            );
        }

        return "redirect:/lead/events/"
                + eventId
                + "/attendees";
    }

    @PostMapping("/members/{id}/remove")
    public String removeActiveMember(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Club club = getLeadClub(userDetails);
        if (club == null) {
            return "lead/no-club";
        }
        try {
            ClubMembership membership = membershipService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Membership not found."));
            if (membership.getClub() == null || !java.util.Objects.equals(membership.getClub().getId(), club.getId())) {
                throw new IllegalArgumentException("You can only remove members from your club.");
            }
            membershipService.removeActiveMember(membership);
            redirectAttributes.addFlashAttribute("successMessage", "Member removed from the club roster.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lead/members";
    }

    @GetMapping("/club/edit")
    public String editClubProfile(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        model.addAttribute("club", club);

        return "lead/club-edit";
    }

    @PostMapping("/club/edit")
    public String updateClubProfile(
            @ModelAttribute("club")
            @Valid
            Club clubForm,

            BindingResult bindingResult,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes
    ) {

        Club currentClub =
                getLeadClub(userDetails);

        if (currentClub == null) {
            return "lead/no-club";
        }

        if (bindingResult.hasErrors()) {
            return "lead/club-edit";
        }

        currentClub.setShortTagline(
                clubForm.getShortTagline()
        );

        currentClub.setDescription(
                clubForm.getDescription()
        );

        currentClub.setFacultyAdvisor(
                clubForm.getFacultyAdvisor()
        );

        currentClub.setRoomVenue(
                clubForm.getRoomVenue()
        );

        currentClub.setMeetingSchedule(
                clubForm.getMeetingSchedule()
        );

        currentClub.setContactEmail(
                clubForm.getContactEmail()
        );

        currentClub.setBannerGradient(
                clubForm.getBannerGradient()
        );

        clubService.saveClub(currentClub);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Club profile updated successfully!"
        );

        return "redirect:/lead/club/edit";
    }

    @PostMapping("/announcements/new")
    public String postAnnouncement(
            @RequestParam("title")
            String title,

            @RequestParam("content")
            String content,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,

            @RequestParam(value = "attachment", required = false) org.springframework.web.multipart.MultipartFile attachment,

            RedirectAttributes redirectAttributes
    ) {

        Club club = getLeadClub(userDetails);

        if (club == null) {
            return "lead/no-club";
        }

        User user = userService
                .findById(userDetails.getId())
                .orElse(userDetails.getUser());

        Announcement announcement =
                new Announcement(
                        title,
                        content,
                        club,
                        AnnouncementPriority.NORMAL,
                        user
                );

        try {
            announcementService.applyAttachments(announcement, image, attachment);
            announcementService.save(announcement);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/lead/dashboard";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Announcement posted to "
                        + club.getName()
                        + " notice board!"
        );

        return "redirect:/lead/dashboard";
    }

    @PostMapping("/announcements/{id}/edit")
    public String updateAnnouncement(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value = "attachment", required = false) org.springframework.web.multipart.MultipartFile attachment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Club club = getLeadClub(userDetails);
        if (club == null) {
            return "lead/no-club";
        }
        try {
            Announcement announcement = announcementService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Notice not found."));
            if (announcement.getClub() == null || !java.util.Objects.equals(announcement.getClub().getId(), club.getId())) {
                throw new IllegalArgumentException("You can only edit notices posted to your club.");
            }
            if (title.isBlank() || content.isBlank()) {
                throw new IllegalArgumentException("Headline and content are required.");
            }
            announcement.setTitle(title.trim());
            announcement.setContent(content.trim());
            announcementService.applyAttachments(announcement, image, attachment);
            announcementService.update(announcement);
            redirectAttributes.addFlashAttribute("successMessage", "Notice updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lead/dashboard";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        Club club = getLeadClub(userDetails);
        if (club == null) {
            return "lead/no-club";
        }
        try {
            Announcement announcement = announcementService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Notice not found."));
            if (announcement.getClub() == null || !java.util.Objects.equals(announcement.getClub().getId(), club.getId())) {
                throw new IllegalArgumentException("You can only delete notices posted to your club.");
            }
            announcementService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Notice deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lead/dashboard";
    }
}
