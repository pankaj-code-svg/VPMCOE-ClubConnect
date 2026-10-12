package com.vpmcoe.clubconnect.service;

import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubCategory;
import com.vpmcoe.clubconnect.model.ClubStatus;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.ClubRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    public ClubService(ClubRepository clubRepository,
                       UserRepository userRepository) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
    }

    public Club saveClub(Club club) {
        return clubRepository.save(club);
    }

    @Transactional(readOnly = true)
    public List<Club> findAllActive() {
        return clubRepository.findByStatus(ClubStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Club> findAll() {
        return clubRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Club> findById(Long id) {
        return clubRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Club> findByCategory(ClubCategory category) {
        return clubRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findByLead(User lead) {
        return clubRepository.findByLead(lead);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findByLeadId(Long leadId) {
        return clubRepository.findByLeadId(leadId);
    }

    @Transactional(readOnly = true)
    public List<Club> searchClubs(String keyword) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            return clubRepository.searchClubs(keyword.trim());
        }

        return findAllActive();
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return clubRepository.count();
    }

    public Club updateClub(Long id, Club updated) {

        Club existing = clubRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Club not found: " + id));

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setShortTagline(updated.getShortTagline());
        existing.setDescription(updated.getDescription());
        existing.setFacultyAdvisor(updated.getFacultyAdvisor());
        existing.setRoomVenue(updated.getRoomVenue());
        existing.setMeetingSchedule(updated.getMeetingSchedule());
        existing.setContactEmail(updated.getContactEmail());
        existing.setBannerGradient(updated.getBannerGradient());

        return clubRepository.save(existing);
    }

    public Club assignLead(Long clubId, Long userId) {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Club not found: " + clubId));

        User previousLead = club.getLead();
        User nextLead = null;
        if (userId != null) {

            nextLead = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "User not found: " + userId));

            if (nextLead.getRole() == Role.ROLE_ADMIN || !nextLead.isEnabled()) {
                throw new IllegalArgumentException("Choose an enabled non-admin user as the club leader.");
            }
        }

        if (nextLead != null) {
            nextLead.setRole(Role.forClubCategory(club.getCategory()));
            userRepository.save(nextLead);
            for (Club otherClub : clubRepository.findAllByLead(nextLead)) {
                if (!otherClub.getId().equals(clubId)) {
                    otherClub.setLead(null);
                    clubRepository.save(otherClub);
                }
            }
        }

        club.setLead(nextLead);
        Club savedClub = clubRepository.saveAndFlush(club);

        if (previousLead != null && (nextLead == null || !previousLead.getId().equals(nextLead.getId()))) {
            for (Club otherClub : clubRepository.findAllByLead(previousLead)) {
                otherClub.setLead(null);
                clubRepository.save(otherClub);
            }
            if (previousLead.getRole().isClubLead()) {
                previousLead.setRole(Role.ROLE_STUDENT);
                userRepository.save(previousLead);
            }
        }

        return savedClub;
    }

    public boolean toggleClubStatus(Long id) {

        Club club = clubRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Club not found: " + id));

        if (club.getStatus() == ClubStatus.ACTIVE) {
            club.setStatus(ClubStatus.INACTIVE);
        } else {
            club.setStatus(ClubStatus.ACTIVE);
        }

        clubRepository.save(club);

        return club.getStatus() == ClubStatus.ACTIVE;
    }
}
