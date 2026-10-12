package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {

    Optional<ClubMembership> findByClubAndStudent(Club club, User student);

    boolean existsByClubAndStudent(Club club, User student);

    List<ClubMembership> findByStudentOrderByAppliedAtDesc(User student);

    List<ClubMembership> findByStudentAndStatus(
            User student,
            MembershipStatus status
    );

    void deleteByStudent(User student);

    List<ClubMembership> findByClubOrderByAppliedAtDesc(Club club);

    List<ClubMembership> findByClubAndStatusOrderByAppliedAtDesc(
            Club club,
            MembershipStatus status
    );

    long countByClubAndStatus(
            Club club,
            MembershipStatus status
    );

    long countByStudentAndStatus(
            User student,
            MembershipStatus status
    );
}
