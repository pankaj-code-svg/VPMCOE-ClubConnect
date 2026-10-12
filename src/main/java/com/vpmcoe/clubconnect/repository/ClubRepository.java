package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubCategory;
import com.vpmcoe.clubconnect.model.ClubStatus;
import com.vpmcoe.clubconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    Optional<Club> findByName(String name);

    List<Club> findByCategory(ClubCategory category);

    List<Club> findByStatus(ClubStatus status);

    Optional<Club> findByLead(User lead);

    List<Club> findAllByLead(User lead);

    Optional<Club> findByLeadId(Long leadId);

    @Query("""
            SELECT c FROM Club c
            WHERE c.status = 'ACTIVE'
            AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.shortTagline) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    List<Club> searchClubs(@Param("keyword") String keyword);
}
