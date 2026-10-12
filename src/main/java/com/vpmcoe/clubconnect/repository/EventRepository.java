package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByClubOrderByEventDateDesc(Club club);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByClubAndStatusOrderByEventDateDesc(
            Club club,
            EventStatus status
    );

    List<Event> findByEventDateAfterOrderByEventDateAsc(
            LocalDateTime dateTime
    );

    List<Event> findByRegistrationDeadlineBetweenAndStatus(
            LocalDateTime from, LocalDateTime to, EventStatus status
    );

    List<Event> findByEventDateBetweenAndStatus(
            LocalDateTime from, LocalDateTime to, EventStatus status
    );

    @Query("""
            SELECT e FROM Event e
            WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.club.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> searchEvents(@Param("keyword") String keyword);
}
