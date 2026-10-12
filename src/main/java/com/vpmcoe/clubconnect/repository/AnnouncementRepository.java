package com.vpmcoe.clubconnect.repository;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findByClubOrderByCreatedAtDesc(Club club);

    List<Announcement> findByClubIsNullOrderByCreatedAtDesc();

    List<Announcement> findTop5ByOrderByCreatedAtDesc();

    List<Announcement> findByPostedBy(User postedBy);
}
