package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.RegistrationStatus;
import com.vpmcoe.clubconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    List<User> findByRegistrationStatus(RegistrationStatus registrationStatus);

    long countByRegistrationStatus(RegistrationStatus registrationStatus);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = com.vpmcoe.clubconnect.model.Role.ROLE_STUDENT
              AND (u.registrationStatus = :status
                   OR (:status = com.vpmcoe.clubconnect.model.RegistrationStatus.PENDING
                   AND u.registrationStatus IS NULL
                   AND u.enabled = false))
            ORDER BY u.createdAt DESC
            """)
    List<User> findRegistrationRequestsByStatus(
            @Param("status") RegistrationStatus status
    );

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = com.vpmcoe.clubconnect.model.Role.ROLE_STUDENT
              AND (u.registrationStatus = :status
                   OR (:status = com.vpmcoe.clubconnect.model.RegistrationStatus.PENDING
                   AND u.registrationStatus IS NULL
                   AND u.enabled = false))
            """)
    long countRegistrationRequestsByStatus(
            @Param("status") RegistrationStatus status
    );

    @Query("SELECT u FROM User u WHERE u.enabled = true AND (u.registrationStatus = com.vpmcoe.clubconnect.model.RegistrationStatus.APPROVED OR u.registrationStatus IS NULL)")
    List<User> findAllApprovedAndEnabled();

    List<User> findByDepartment(String department);
}
