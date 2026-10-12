package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.NotificationType;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.AnnouncementRepository;
import com.vpmcoe.clubconnect.repository.EmailNotificationLogRepository;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final ClubMembershipRepository membershipRepository;
    private final EmailService emailService;
    private final EmailNotificationLogRepository notificationLogRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               ClubMembershipRepository membershipRepository,
                               EmailService emailService,
                               EmailNotificationLogRepository notificationLogRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.emailService = emailService;
        this.notificationLogRepository = notificationLogRepository;
    }

    public Announcement save(Announcement announcement) {
        Announcement saved = announcementRepository.save(announcement);
        if (saved.getClub() == null) {
            userRepository.findAllApprovedAndEnabled().forEach(user ->
                    emailService.sendAnnouncementEmail(user, saved, NotificationType.CAMPUS_ANNOUNCEMENT));
        } else {
            membershipRepository.findByClubAndStatusOrderByAppliedAtDesc(saved.getClub(), MembershipStatus.APPROVED)
                    .stream().map(ClubMembership::getStudent)
                    .filter(User::isEnabled).filter(User::isRegistrationApproved)
                    .forEach(user -> emailService.sendAnnouncementEmail(user, saved, NotificationType.CLUB_ANNOUNCEMENT));
        }
        return saved;
    }

    public Announcement update(Announcement announcement) {
        return announcementRepository.save(announcement);
    }

    public void applyAttachments(Announcement announcement, MultipartFile image, MultipartFile attachment) {
        try {
            if (image != null && !image.isEmpty()) {
                String type = image.getContentType();
                if (type == null || !List.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
                    throw new IllegalArgumentException("Notice image must be JPG, PNG, WEBP, or GIF.");
                }
                if (image.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("Notice image must be 5 MB or smaller.");
                }
                announcement.setImageData(image.getBytes());
                announcement.setImageContentType(type);
                announcement.setImageFileName(cleanFileName(image.getOriginalFilename()));
            }
            if (attachment != null && !attachment.isEmpty()) {
                String type = attachment.getContentType();
                if (!"application/pdf".equals(type)) {
                    throw new IllegalArgumentException("Notice attachment must be a PDF file.");
                }
                if (attachment.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("Notice PDF must be 10 MB or smaller.");
                }
                announcement.setAttachmentData(attachment.getBytes());
                announcement.setAttachmentContentType(type);
                announcement.setAttachmentFileName(cleanFileName(attachment.getOriginalFilename()));
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read the notice attachment.", e);
        }
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "attachment";
        String normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_");
    }

    @Transactional(readOnly = true)
    public List<Announcement> findAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Announcement> findRecent() {
        return announcementRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Announcement> findByClub(Club club) {
        return announcementRepository.findByClubOrderByCreatedAtDesc(club);
    }

    @Transactional(readOnly = true)
    public List<Announcement> findCampusWide() {
        return announcementRepository.findByClubIsNullOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public void delete(Long id) {
        announcementRepository.findById(id).ifPresent(announcement -> {
            notificationLogRepository.deleteByAnnouncement(announcement);
            announcementRepository.delete(announcement);
        });
    }
}
