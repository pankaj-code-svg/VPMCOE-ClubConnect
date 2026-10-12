package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.security.CustomUserDetails;
import com.vpmcoe.clubconnect.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfilePhotoController {

    private final UserService userService;

    public ProfilePhotoController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile-photo/{userId}")
    public ResponseEntity<byte[]> profilePhoto(@PathVariable Long userId,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        User user = userService.findById(userId).orElse(null);
        if (user == null || user.getProfilePhoto() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(user.getProfilePhotoContentType());
        } catch (Exception ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(user.getProfilePhoto());
    }

    @PostMapping("/profile-photo/update")
    public String updateProfilePhoto(@RequestParam("profilePhoto") MultipartFile profilePhoto,
                                     @AuthenticationPrincipal CustomUserDetails currentUser,
                                     RedirectAttributes redirectAttributes) {
        String profilePath = currentUser.getUser().getRole() == com.vpmcoe.clubconnect.model.Role.ROLE_ADMIN
                ? "/admin/profile"
                : currentUser.getUser().getRole().isClubLead() ? "/lead/profile" : "/student/profile";
        try {
            userService.updateProfilePhoto(currentUser.getId(), profilePhoto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + profilePath;
    }
}
