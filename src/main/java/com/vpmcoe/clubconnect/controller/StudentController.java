package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.DashboardService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.MembershipService;
import com.vpmcoe.clubconnect.service.UserService;
import com.vpmcoe.clubconnect.service.EventPassPdfService;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final DashboardService dashboardService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final ClubService clubService;
    private final UserService userService;
    private final EventPassPdfService eventPassPdfService;

    public StudentController(
            DashboardService dashboardService,
            MembershipService membershipService,
            EventService eventService,
            ClubService clubService,
            UserService userService,
            EventPassPdfService eventPassPdfService) {

        this.dashboardService = dashboardService;
        this.membershipService = membershipService;
        this.eventService = eventService;
        this.clubService = clubService;
        this.userService = userService;
        this.eventPassPdfService = eventPassPdfService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        User student = userDetails.getUser();

        student = userService.findById(student.getId())
                .orElse(student);

        Map<String, Object> stats =
                dashboardService.getStudentDashboardStats(student);

        model.addAllAttributes(stats);
        model.addAttribute("currentUser", student);

        return "student/dashboard";
    }

    @PostMapping("/clubs/{id}/apply")
    public String applyForClub(
            @PathVariable Long id,

            @RequestParam("applicationReason")
            String applicationReason,

            @RequestParam(value = "skills", required = false)
            String skills,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        User student = userDetails.getUser();

        Optional<Club> clubOpt =
                clubService.findById(id);

        if (clubOpt.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Club not found."
            );

            return "redirect:/clubs";
        }

        try {

            membershipService.applyForClub(
                    clubOpt.get(),
                    student,
                    applicationReason,
                    skills
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Application submitted successfully to "
                            + clubOpt.get().getName()
                            + "! The club lead will review your application soon."
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/clubs/" + id;
    }

    @PostMapping("/events/{id}/register")
    public String registerForEvent(
            @PathVariable Long id,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        User student = userDetails.getUser();

        Optional<Event> eventOpt =
                eventService.findById(id);

        if (eventOpt.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Event not found."
            );

            return "redirect:/events";
        }

        try {

            if (!eventService.isActive(eventOpt.get())) {
                throw new IllegalArgumentException("This event is no longer available.");
            }
            if (!eventOpt.get().isVisibleToAll()
                    && !membershipService.isApprovedMember(eventOpt.get().getClub(), student)) {
                throw new IllegalArgumentException("Join this club to register for its events.");
            }

            EventRegistration registration =
                    eventService.registerForEvent(
                            eventOpt.get(),
                            student
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Registration confirmed! Here is your entry pass."
            );

            return "redirect:/student/tickets/"
                    + registration.getId();

        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/events/" + id;
        }
    }

    @GetMapping("/tickets/{regId}")
    public String viewTicket(
            @PathVariable Long regId,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<EventRegistration> regOpt =
                eventService.getRegistrationById(regId);

        if (regOpt.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Ticket not found."
            );

            return "redirect:/student/dashboard";
        }

        EventRegistration registration =
                regOpt.get();

        boolean isOwner = registration.getStudent()
                .getId()
                .equals(userDetails.getUser().getId());

        boolean isAdmin = userDetails.getUser()
                .getRole()
                .name()
                .equals("ROLE_ADMIN");

        if (!isOwner && !isAdmin) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unauthorized access."
            );

            return "redirect:/student/dashboard";
        }

        model.addAttribute(
                "registration",
                registration
        );

        model.addAttribute(
                "event",
                registration.getEvent()
        );

        model.addAttribute(
                "student",
                registration.getStudent()
        );

        return "student/ticket";
    }

    @GetMapping(value = "/tickets/{regId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> ticketQr(@PathVariable Long regId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<EventRegistration> registration = eventService.getRegistrationById(regId);
        if (registration.isEmpty()) return ResponseEntity.notFound().build();
        User currentUser = userDetails.getUser();
        boolean isOwner = registration.get().getStudent().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == com.vpmcoe.clubconnect.model.Role.ROLE_ADMIN;
        if (!isOwner && !isAdmin) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
                .body(eventPassPdfService.createQrCode(registration.get().getTicketNumber(), 280));
    }

    @GetMapping("/profile")
    public String profilePage(
            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            Model model) {

        User user = userService.findById(
                userDetails.getId()
        ).orElse(userDetails.getUser());

        model.addAttribute("user", user);

        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam("fullName")
            String fullName,

            @RequestParam("prn")
            String prn,

            @RequestParam("department")
            String department,

            @RequestParam("academicYear")
            String academicYear,

            @RequestParam("phone")
            String phone,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {

            userService.updateProfile(
                    userDetails.getId(),
                    fullName,
                    prn,
                    department,
                    academicYear,
                    phone
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Profile updated successfully!"
            );

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error updating profile: " + e.getMessage()
            );
        }

        return "redirect:/student/profile";
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
        return "redirect:/student/profile";
    }
}
