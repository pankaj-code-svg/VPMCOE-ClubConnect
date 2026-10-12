package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.ClubRole;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MembershipService {

    private final ClubMembershipRepository membershipRepository;

    public MembershipService(
            ClubMembershipRepository membershipRepository
    ) {
        this.membershipRepository = membershipRepository;
    }

    public ClubMembership applyForClub(
            Club club,
            User student,
            String applicationReason,
            String skills
    ) {

        Optional<ClubMembership> existing =
                membershipRepository.findByClubAndStudent(
                        club,
                        student
                );

        if (existing.isPresent()) {

            ClubMembership membership = existing.get();

            if (membership.getStatus()
                    == MembershipStatus.REJECTED) {

                membership.setStatus(
                        MembershipStatus.PENDING
                );

                membership.setApplicationReason(
                        applicationReason
                );

                membership.setSkills(skills);

                membership.setAppliedAt(
                        LocalDateTime.now()
                );

                membership.setReviewedAt(null);
                membership.setReviewNotes(null);

                return membershipRepository.save(membership);

            } else {

                throw new IllegalArgumentException(
                        "You already have an active or pending membership "
                                + "in this club."
                );
            }
        }

        ClubMembership membership =
                new ClubMembership(
                        club,
                        student,
                        MembershipStatus.PENDING,
                        ClubRole.GENERAL_MEMBER,
                        applicationReason,
                        skills
                );

        return membershipRepository.save(membership);
    }

    public ClubMembership approveMembership(
            Long membershipId,
            ClubRole role,
            String reviewNotes
    ) {

        ClubMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Membership not found: "
                                                + membershipId
                                ));

        membership.setStatus(
                MembershipStatus.APPROVED
        );

        if (role != null) {
            membership.setClubRole(role);
        }

        membership.setReviewedAt(
                LocalDateTime.now()
        );

        membership.setReviewNotes(reviewNotes);

        return membershipRepository.save(membership);
    }

    public ClubMembership rejectMembership(
            Long membershipId,
            String reviewNotes
    ) {

        ClubMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Membership not found: "
                                                + membershipId
                                ));

        membership.setStatus(
                MembershipStatus.REJECTED
        );

        membership.setReviewedAt(
                LocalDateTime.now()
        );

        membership.setReviewNotes(reviewNotes);

        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> findUserMemberships(
            User student
    ) {
        return membershipRepository
                .findByStudentOrderByAppliedAtDesc(student);
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> findApprovedUserMemberships(
            User student
    ) {
        return membershipRepository
                .findByStudentAndStatus(
                        student,
                        MembershipStatus.APPROVED
                );
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> findClubMemberships(
            Club club
    ) {
        return membershipRepository
                .findByClubOrderByAppliedAtDesc(club);
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> findPendingMemberships(
            Club club
    ) {
        return membershipRepository
                .findByClubAndStatusOrderByAppliedAtDesc(
                        club,
                        MembershipStatus.PENDING
                );
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> findActiveMembers(
            Club club
    ) {
        return membershipRepository
                .findByClubAndStatusOrderByAppliedAtDesc(
                        club,
                        MembershipStatus.APPROVED
                );
    }

    @Transactional(readOnly = true)
    public Optional<ClubMembership> getMembership(
            Club club,
            User student
    ) {
        return membershipRepository
                .findByClubAndStudent(club, student);
    }

    @Transactional(readOnly = true)
    public boolean isApprovedMember(
            Club club,
            User student
    ) {

        Optional<ClubMembership> membership =
                membershipRepository
                        .findByClubAndStudent(club, student);

        return membership.isPresent()
                && membership.get().getStatus()
                == MembershipStatus.APPROVED;
    }

    @Transactional(readOnly = true)
    public long countActiveMembers(
            Club club
    ) {
        return membershipRepository
                .countByClubAndStatus(
                        club,
                        MembershipStatus.APPROVED
                );
    }

    @Transactional(readOnly = true)
    public long countPendingRequests(
            Club club
    ) {
        return membershipRepository
                .countByClubAndStatus(
                        club,
                        MembershipStatus.PENDING
                );
    }

    @Transactional(readOnly = true)
    public Optional<ClubMembership> findById(
            Long id
    ) {
        return membershipRepository.findById(id);
    }

    public void removeActiveMember(ClubMembership membership) {
        if (membership.getStatus() != MembershipStatus.APPROVED
                || membership.getClubRole() == ClubRole.LEAD) {
            throw new IllegalArgumentException("Only active club members can be removed here.");
        }
        membershipRepository.delete(membership);
    }
}
