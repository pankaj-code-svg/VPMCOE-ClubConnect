package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.AttendanceIncident;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceIncidentRepository extends JpaRepository<AttendanceIncident, Long> {
    List<AttendanceIncident> findAllByOrderByCreatedAtDesc();
    List<AttendanceIncident> findByReporterOrderByCreatedAtDesc(User reporter);
    List<AttendanceIncident> findByScanningClubOrderByCreatedAtDesc(Club scanningClub);
    void deleteByReporter(User reporter);
    void deleteByRegistrationStudent(User student);
}
