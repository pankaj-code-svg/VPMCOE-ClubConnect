package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByEventAndStudent(
            Event event,
            User student
    );

    boolean existsByEventAndStudent(
            Event event,
            User student
    );

    List<EventRegistration> findByStudentOrderByRegisteredAtDesc(
            User student
    );

    void deleteByStudent(User student);

    void deleteByEvent(Event event);

    List<EventRegistration> findByEventOrderByRegisteredAtDesc(
            Event event
    );

    long countByEvent(Event event);

    long countByEventAndAttendedTrue(Event event);

    Optional<EventRegistration> findByTicketNumber(
            String ticketNumber
    );

    long countByStudent(User student);

    List<EventRegistration> findByEvent(Event event);

    List<EventRegistration> findByAttendedTrueOrderByAttendedAtDesc();

    List<EventRegistration> findByEventClubAndAttendedTrueOrderByAttendedAtDesc(Club club);
}
