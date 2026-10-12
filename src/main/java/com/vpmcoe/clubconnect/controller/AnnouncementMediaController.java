package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.AnnouncementService;
import com.vpmcoe.clubconnect.service.ClubService;
import com.vpmcoe.clubconnect.service.EventService;
import com.vpmcoe.clubconnect.service.MembershipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AnnouncementMediaController {

    private final AnnouncementService announcementService;
    private final EventService eventService;
    private final MembershipService membershipService;
    private final ClubService clubService;

    public AnnouncementMediaController(AnnouncementService announcementService, EventService eventService,
                                      MembershipService membershipService, ClubService clubService) {
        this.announcementService = announcementService;
        this.eventService = eventService;
        this.membershipService = membershipService;
        this.clubService = clubService;
    }

    @GetMapping("/announcements/{id}/image")
    public ResponseEntity<byte[]> announcementImage(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        Announcement announcement = announcementService.findById(id).orElse(null);
        if (announcement == null || announcement.getImageData() == null) return ResponseEntity.notFound().build();
        if (!canView(announcement, userDetails)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(mediaType(announcement.getImageContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(announcement.getImageData());
    }

    @GetMapping("/announcements/{id}/attachment")
    public ResponseEntity<byte[]> announcementAttachment(@PathVariable Long id,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        Announcement announcement = announcementService.findById(id).orElse(null);
        if (announcement == null || announcement.getAttachmentData() == null) return ResponseEntity.notFound().build();
        if (!canView(announcement, userDetails)) return ResponseEntity.notFound().build();
        String filename = announcement.getAttachmentFileName() == null ? "notice.pdf" : announcement.getAttachmentFileName();
        return ResponseEntity.ok().contentType(mediaType(announcement.getAttachmentContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename.replace("\"", "") + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(announcement.getAttachmentData());
    }

    @GetMapping("/events/{id}/poster")
    public ResponseEntity<byte[]> eventPoster(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Event event = eventService.findById(id).orElse(null);
        if (event == null || event.getPosterImageData() == null || !eventService.isActive(event)
                || !canViewEvent(event, userDetails)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(mediaType(event.getPosterImageContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(event.getPosterImageData());
    }

    private MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean canView(Announcement announcement, CustomUserDetails userDetails) {
        if (announcement.getClub() == null) return true;
        if (userDetails == null) return false;
        User user = userDetails.getUser();
        if (user.getRole() == Role.ROLE_ADMIN || membershipService.isApprovedMember(announcement.getClub(), user)) return true;
        return clubService.findByLead(user)
                .map(club -> java.util.Objects.equals(club.getId(), announcement.getClub().getId()))
                .orElse(false);
    }

    private boolean canViewEvent(Event event, CustomUserDetails userDetails) {
        if (event.isVisibleToAll()) return true;
        if (userDetails == null) return false;
        User user = userDetails.getUser();
        if (user.getRole() == Role.ROLE_ADMIN || membershipService.isApprovedMember(event.getClub(), user)) return true;
        return clubService.findByLead(user)
                .map(club -> java.util.Objects.equals(club.getId(), event.getClub().getId()))
                .orElse(false);
    }
}
