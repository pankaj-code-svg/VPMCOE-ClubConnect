package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.AnnouncementPriority;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubCategory;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventMode;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.AnnouncementService;
import com.vpmcoe.clubconnect.service.AttendanceService;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.DashboardService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.UserService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final ClubService clubService;
    private final UserService userService;
    private final AnnouncementService announcementService;
    private final EventService eventService;
    private final AttendanceService attendanceService;

    public AdminController(
            DashboardService dashboardService,
            ClubService clubService,
            UserService userService,
            AnnouncementService announcementService,
            EventService eventService,
            AttendanceService attendanceService
    ) {
        this.dashboardService = dashboardService;
        this.clubService = clubService;
        this.userService = userService;
        this.announcementService = announcementService;
        this.eventService = eventService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        Map<String, Object> stats =
                dashboardService.getAdminDashboardStats();

        model.addAllAttributes(stats);

        model.addAttribute(
                "recentAnnouncements",
                announcementService.findRecent()
        );

        return "admin/dashboard";
    }

    @GetMapping("/attendance")
    public String attendanceRecords(Model model) {
        model.addAttribute("records", attendanceService.findAllAttendance());
        model.addAttribute("incidents", attendanceService.findAllIncidents());
        return "admin/attendance";
    }

    @PostMapping("/attendance/incidents/{id}/resolve")
    public String resolveAttendanceIncident(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            attendanceService.setIncidentResolved(id, true);
            redirectAttributes.addFlashAttribute("successMessage", "Attendance incident marked as reviewed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/attendance";
    }

    @GetMapping("/events/new")
    public String newEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("clubs", clubService.findAllActive());
        model.addAttribute("modes", EventMode.values());
        return "admin/event-form";
    }

    @PostMapping("/events/new")
    public String createPublicEvent(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("clubId") Long clubId,
            @RequestParam("eventDate") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime eventDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
            @RequestParam("venue") String venue,
            @RequestParam("mode") EventMode mode,
            @RequestParam(value = "onlineLink", required = false) String onlineLink,
            @RequestParam(value = "registrationDeadline", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime registrationDeadline,
            @RequestParam("maxCapacity") int maxCapacity,
            @RequestParam(value = "poster", required = false) MultipartFile poster,
            RedirectAttributes redirectAttributes
    ) {
        Club club = clubService.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found."));
        Event event = new Event(title, description, club, eventDate, endDate, venue, mode,
                onlineLink, registrationDeadline, maxCapacity, null);
        event.setVisibleToAll(true);
        try {
            eventService.saveEvent(event, poster);
            redirectAttributes.addFlashAttribute("successMessage", "Public campus event published.");
            return "redirect:/admin/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/events/new";
        }
    }

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {

        User user = userService
                .findById(userDetails.getId())
                .orElse(userDetails.getUser());

        model.addAttribute("user", user);

        return "admin/profile";
    }

    @GetMapping("/clubs")
    public String clubs(Model model) {

        model.addAttribute(
                "clubs",
                clubService.findAll()
        );
        model.addAttribute("users", userService.findAll());

        return "admin/clubs";
    }

    @PostMapping("/clubs/{id}/lead")
    public String changeClubLead(
            @PathVariable Long id,
            @RequestParam(value = "leadUserId", required = false) Long leadUserId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Club club = clubService.assignLead(id, leadUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    club.getLead() == null
                            ? "Club leader removed. The club is now vacant."
                            : "Club leader updated to " + club.getLead().getFullName()
                            + ". Their existing profile details were preserved. They should sign out and back in to refresh portal access.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/clubs";
    }

    @GetMapping("/clubs/new")
    public String newClubForm(Model model) {

        model.addAttribute(
                "club",
                new Club()
        );

        model.addAttribute(
                "categories",
                ClubCategory.values()
        );

        model.addAttribute(
                "users",
                userService.findAll()
        );

        return "admin/club-form";
    }

    @PostMapping("/clubs/new")
    public String createClub(
            @ModelAttribute("club") @Valid Club club,
            BindingResult bindingResult,
            @RequestParam(
                    value = "leadUserId",
                    required = false
            )
            Long leadUserId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "categories",
                    ClubCategory.values()
            );

            model.addAttribute(
                    "users",
                    userService.findAll()
            );

            return "admin/club-form";
        }

        Club savedClub = clubService.saveClub(club);
        if (leadUserId != null) {
            clubService.assignLead(savedClub.getId(), leadUserId);
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Club '" + club.getName()
                        + "' created successfully!"
        );

        return "redirect:/admin/clubs";
    }

    @GetMapping("/clubs/{id}/edit")
    public String editClubForm(
            @PathVariable Long id,
            Model model
    ) {

        Club club = clubService
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Club not found: " + id
                        )
                );

        model.addAttribute(
                "club",
                club
        );

        model.addAttribute(
                "categories",
                ClubCategory.values()
        );

        model.addAttribute(
                "users",
                userService.findAll()
        );

        return "admin/club-form";
    }

    @PostMapping("/clubs/{id}/edit")
    public String updateClub(
            @PathVariable Long id,
            @ModelAttribute("club")
            @Valid
            Club clubForm,
            BindingResult bindingResult,
            @RequestParam(
                    value = "leadUserId",
                    required = false
            )
            Long leadUserId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "categories",
                    ClubCategory.values()
            );

            model.addAttribute(
                    "users",
                    userService.findAll()
            );

            return "admin/club-form";
        }

        clubService.updateClub(
                id,
                clubForm
        );

        clubService.assignLead(
                id,
                leadUserId
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Club '" + clubForm.getName()
                        + "' updated successfully!"
        );

        return "redirect:/admin/clubs";
    }

    @PostMapping("/clubs/{id}/toggle-status")
    public String toggleClubStatus(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            boolean active =
                    clubService.toggleClubStatus(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Club status updated to "
                            + (active
                            ? "ACTIVE"
                            : "INACTIVE")
                            + "."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error changing club status: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/clubs";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(
                    value = "role",
                    required = false
            )
            String roleStr,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {

        List<User> users;

        if (roleStr != null
                && !roleStr.isBlank()) {

            try {

                Role role =
                        Role.valueOf(roleStr);

                users =
                        userService.findByRole(role);

                model.addAttribute(
                        "selectedRole",
                        roleStr
                );

            } catch (Exception e) {

                users =
                        userService.findAll();
            }

        } else {

            users =
                    userService.findAll();
        }

        model.addAttribute(
                "users",
                users
        );

        model.addAttribute(
                "roles",
                Role.values()
        );
        model.addAttribute("roleOptions", List.of(Role.ROLE_ADMIN, Role.ROLE_STUDENT));
        model.addAttribute("currentUserId", userDetails.getId());

        return "admin/users";
    }

    @GetMapping("/registrations")
    public String registrationRequests(Model model) {
        model.addAttribute(
                "registrations",
                userService.findPendingRegistrations()
        );
        return "admin/registrations";
    }

    @PostMapping("/registrations/{id}/approve")
    public String approveRegistration(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.approveRegistration(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User registration approved successfully. The user can now log in."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @PostMapping("/registrations/{id}/reject")
    public String rejectRegistration(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.rejectRegistration(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User registration rejected and removed successfully."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @PostMapping("/users/{id}/change-role")
    public String changeUserRole(
            @PathVariable Long id,
            @RequestParam("role")
            Role role,
            RedirectAttributes redirectAttributes
    ) {

        try {

            userService.changeUserRole(
                    id,
                    role
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User role updated successfully!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating role: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            boolean enabled =
                    userService.toggleUserStatus(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User account is now "
                            + (enabled
                            ? "ENABLED"
                            : "DISABLED")
                            + "."
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating user status: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUserPermanently(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete the admin account currently in use.");
            return "redirect:/admin/users";
        }
        try {
            userService.deleteUserPermanently(id);
            redirectAttributes.addFlashAttribute("successMessage", "User permanently removed. Their email address can be used to register again.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/announcements")
    public String announcements(Model model) {

        model.addAttribute(
                "announcements",
                announcementService.findAll()
        );

        model.addAttribute(
                "priorities",
                AnnouncementPriority.values()
        );

        return "admin/announcements";
    }

    @PostMapping("/announcements/new")
    public String postCampusAnnouncement(
            @RequestParam("title")
            String title,
            @RequestParam("content")
            String content,
            @RequestParam(
                    value = "priority",
                    defaultValue = "NORMAL"
            )
            AnnouncementPriority priority,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value = "attachment", required = false) org.springframework.web.multipart.MultipartFile attachment,
            @AuthenticationPrincipal
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {

        User user =
                userService
                        .findById(
                                userDetails.getId()
                        )
                        .orElse(
                                userDetails.getUser()
                        );

        Announcement announcement =
                new Announcement(
                        title,
                        content,
                        null,
                        priority,
                        user
                );

        try {
            announcementService.applyAttachments(announcement, image, attachment);
            announcementService.save(announcement);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/announcements";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Campus-wide announcement published!"
        );

        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/{id}/edit")
    public String updateAnnouncement(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("priority") AnnouncementPriority priority,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value = "attachment", required = false) org.springframework.web.multipart.MultipartFile attachment,
            RedirectAttributes redirectAttributes
    ) {
        Announcement announcement = announcementService.findById(id).orElse(null);
        if (announcement == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Notice not found.");
            return "redirect:/admin/announcements";
        }
        if (announcement.getClub() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only campus-wide notices can be edited here.");
            return "redirect:/admin/announcements";
        }
        if (title.isBlank() || content.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Headline and announcement details are required.");
            return "redirect:/admin/announcements";
        }

        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setPriority(priority);
        try {
            announcementService.applyAttachments(announcement, image, attachment);
            announcementService.update(announcement);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/announcements";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Campus notice updated successfully.");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        Announcement announcement = announcementService.findById(id).orElse(null);
        if (announcement == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Notice not found.");
            return "redirect:/admin/announcements";
        }
        if (announcement.getClub() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only campus-wide notices can be deleted here.");
            return "redirect:/admin/announcements";
        }

        announcementService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Campus notice deleted successfully.");
        return "redirect:/admin/announcements";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            RedirectAttributes redirectAttributes
    ) {

        try {

            userService.updateAdminProfile(
                    userDetails.getId(),
                    fullName,
                    phone
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Admin profile updated successfully!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating profile: " + e.getMessage()
            );
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/profile/account")
    public String updateAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("email") String email,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmNewPassword", required = false) String confirmNewPassword,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        try {
            boolean credentialsChanged = !email.trim().equalsIgnoreCase(userDetails.getUsername())
                    || (newPassword != null && !newPassword.isBlank());
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
        return "redirect:/admin/profile";
    }
}
