package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.RegistrationStatus;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.repository.AnnouncementRepository;
import com.vpmcoe.clubconnect.repository.AttendanceIncidentRepository;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import com.vpmcoe.clubconnect.repository.ClubRepository;
import com.vpmcoe.clubconnect.repository.EmailNotificationLogRepository;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ClubMembershipRepository membershipRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EmailNotificationLogRepository notificationLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AnnouncementRepository announcementRepository;
    private final ClubRepository clubRepository;
    private final AttendanceIncidentRepository attendanceIncidentRepository;

    public UserService(UserRepository userRepository,
                       ClubMembershipRepository membershipRepository,
                       EventRegistrationRepository eventRegistrationRepository,
                       EmailNotificationLogRepository notificationLogRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       AnnouncementRepository announcementRepository,
                       ClubRepository clubRepository,
                       AttendanceIncidentRepository attendanceIncidentRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.announcementRepository = announcementRepository;
        this.clubRepository = clubRepository;
        this.attendanceIncidentRepository = attendanceIncidentRepository;
    }

    public User registerStudent(User user) {
        String email = user.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with email " + user.getEmail() + " already exists.");
        }
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ROLE_STUDENT);
        user.setEnabled(false);
        user.setRegistrationStatus(RegistrationStatus.PENDING);
        User registeredUser = userRepository.save(user);
        emailService.sendRegistrationSubmittedEmail(registeredUser);
        return registeredUser;
    }

    public User createUserWithRole(User user, Role role) {
        String email = user.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);
        user.setEnabled(true);
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) { return userRepository.findById(id); }

    @Transactional(readOnly = true)
    public List<User> findAll() { return userRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) { return userRepository.findByRole(role); }

    @Transactional(readOnly = true)
    public long countAll() { return userRepository.count(); }

    @Transactional(readOnly = true)
    public long countByRole(Role role) { return userRepository.countByRole(role); }

    @Transactional(readOnly = true)
    public List<User> findByRegistrationStatus(RegistrationStatus registrationStatus) {
        return userRepository.findByRegistrationStatus(registrationStatus);
    }

    @Transactional(readOnly = true)
    public List<User> findPendingRegistrations() {
        return userRepository.findRegistrationRequestsByStatus(
                RegistrationStatus.PENDING
        );
    }

    @Transactional(readOnly = true)
    public long countByRegistrationStatus(RegistrationStatus registrationStatus) {
        if (registrationStatus == RegistrationStatus.PENDING) {
            return userRepository.countRegistrationRequestsByStatus(
                    RegistrationStatus.PENDING
            );
        }
        return userRepository.countByRegistrationStatus(registrationStatus);
    }

    public User approveRegistration(Long userId) {
        User user = findPendingUser(userId);
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
        user.setEnabled(true);
        User approvedUser = userRepository.save(user);
        emailService.sendRegistrationApprovedEmail(approvedUser);
        return approvedUser;
    }

    public void rejectRegistration(Long userId) {
        User user = findPendingUser(userId);
        emailService.sendRegistrationRejectedEmail(user);
        notificationLogRepository.deleteByUser(user);
        eventRegistrationRepository.deleteByStudent(user);
        membershipRepository.deleteByStudent(user);
        userRepository.delete(user);
        userRepository.flush();
    }

    private User findPendingUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request was not found."));
        if (user.getRole() != Role.ROLE_STUDENT || !isPendingRegistration(user)) {
            throw new IllegalArgumentException("This registration request is no longer pending.");
        }
        return user;
    }

    private boolean isPendingRegistration(User user) {
        return user.getRegistrationStatus() == RegistrationStatus.PENDING
                || (user.getRegistrationStatus() == null && !user.isEnabled());
    }

    public User updateProfile(Long userId, String fullName, String prn, String department,
                              String academicYear, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFullName(fullName);
        user.setPrn(prn);
        user.setDepartment(department);
        user.setAcademicYear(academicYear);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public User updateAdminProfile(Long userId, String fullName, String phone) {
        return updateBasicProfile(userId, fullName, phone);
    }

    public User changeUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (newRole.isClubLead()) {
            throw new IllegalArgumentException("Assign club leader roles through the club directory so the role matches the club.");
        }
        if (user.getRole().isClubLead()) {
            for (Club club : clubRepository.findAllByLead(user)) {
                club.setLead(null);
                clubRepository.save(club);
            }
        }
        user.setRole(newRole);
        return userRepository.save(user);
    }

    public boolean toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return user.isEnabled();
    }

    public void deleteUserPermanently(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        attendanceIncidentRepository.deleteByReporter(user);
        attendanceIncidentRepository.deleteByRegistrationStudent(user);
        eventRegistrationRepository.deleteByStudent(user);
        membershipRepository.deleteByStudent(user);
        notificationLogRepository.deleteByUser(user);

        for (var announcement : announcementRepository.findByPostedBy(user)) {
            notificationLogRepository.deleteByAnnouncement(announcement);
            announcementRepository.delete(announcement);
        }

        for (Club club : clubRepository.findAllByLead(user)) {
            club.setLead(null);
            clubRepository.save(club);
        }

        userRepository.delete(user);
        userRepository.flush();
    }

    public User updateBasicProfile(Long userId, String fullName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFullName(fullName);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public User updateProfilePhoto(Long userId, MultipartFile profilePhoto) {
        if (profilePhoto == null || profilePhoto.isEmpty()) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        }

        String contentType = profilePhoto.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType)) {
            throw new IllegalArgumentException("Please upload a JPG, PNG, WEBP, or GIF image.");
        }
        if (profilePhoto.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile photo must be 5 MB or smaller.");
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            user.setProfilePhoto(profilePhoto.getBytes());
            user.setProfilePhotoContentType(contentType);
            return userRepository.save(user);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded profile photo.", e);
        }
    }

    public User updateAccountCredentials(Long userId, String email, String currentPassword,
                                         String newPassword, String confirmNewPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        boolean emailChanged = !normalizedEmail.equals(user.getEmail());
        boolean passwordChanged = newPassword != null && !newPassword.isBlank();

        if (!emailChanged && !passwordChanged) {
            return user;
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Enter your current password to make account changes.");
        }
        if (emailChanged) {
            if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Please provide a valid email address.");
            }
            userRepository.findByEmail(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> { throw new IllegalArgumentException("That email address is already in use."); });
            user.setEmail(normalizedEmail);
        }
        if (passwordChanged) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters.");
            }
            if (!newPassword.equals(confirmNewPassword)) {
                throw new IllegalArgumentException("New password and confirmation do not match.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        return userRepository.save(user);
    }
}
