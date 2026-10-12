package com.vpmcoe.clubconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid college email")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String prn;

    @Column(nullable = true)
    private String department;

    @Column(nullable = true)
    private String academicYear;

    @Column(nullable = true)
    private String phone;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true, columnDefinition = "LONGBLOB")
    private byte[] profilePhoto;

    @Column(nullable = true)
    private String profilePhotoContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RegistrationStatus registrationStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {
        this.role = Role.ROLE_STUDENT;
        this.enabled = true;
        this.registrationStatus = RegistrationStatus.APPROVED;
        this.createdAt = LocalDateTime.now();
    }

    public User(
            String fullName,
            String email,
            String password,
            String prn,
            String department,
            String academicYear,
            String phone,
            Role role
    ) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.prn = prn;
        this.department = department;
        this.academicYear = academicYear;
        this.phone = phone;
        this.role = role;
        this.enabled = true;
        this.registrationStatus = RegistrationStatus.APPROVED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrn() {
        return prn;
    }

    public void setPrn(String prn) {
        this.prn = prn;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public byte[] getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(byte[] profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getProfilePhotoContentType() {
        return profilePhotoContentType;
    }

    public void setProfilePhotoContentType(String profilePhotoContentType) {
        this.profilePhotoContentType = profilePhotoContentType;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public boolean isRegistrationApproved() {
        return registrationStatus == null
                || registrationStatus == RegistrationStatus.APPROVED;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getInitials() {

        if (fullName != null && !fullName.trim().isEmpty()) {

            String[] parts = fullName.trim().split("\\s+");

            if (parts.length == 1) {
                return parts[0]
                        .substring(0, Math.min(2, parts[0].length()))
                        .toUpperCase();
            }

            return (
                    "" +
                            parts[0].charAt(0) +
                            parts[parts.length - 1].charAt(0)
            ).toUpperCase();
        }

        return "CC";
    }
}
