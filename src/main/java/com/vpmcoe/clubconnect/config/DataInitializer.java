package com.vpmcoe.clubconnect.config;

import com.vpmcoe.clubconnect.model.Announcement;
import com.vpmcoe.clubconnect.model.AnnouncementPriority;
import com.vpmcoe.clubconnect.model.Club;
import com.vpmcoe.clubconnect.model.ClubCategory;
import com.vpmcoe.clubconnect.model.ClubMembership;
import com.vpmcoe.clubconnect.model.ClubRole;
import com.vpmcoe.clubconnect.model.Event;
import com.vpmcoe.clubconnect.model.EventMode;
import com.vpmcoe.clubconnect.model.EventRegistration;
import com.vpmcoe.clubconnect.model.MembershipStatus;
import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.AnnouncementRepository;
import com.vpmcoe.clubconnect.repository.ClubMembershipRepository;
import com.vpmcoe.clubconnect.repository.ClubRepository;
import com.vpmcoe.clubconnect.repository.EventRegistrationRepository;
import com.vpmcoe.clubconnect.repository.EventRepository;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "clubconnect.seed-data.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final AnnouncementRepository announcementRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ClubRepository clubRepository,
            ClubMembershipRepository membershipRepository,
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            AnnouncementRepository announcementRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.announcementRepository = announcementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.count() > 0) {
            for (Club club : clubRepository.findAll()) {
                User lead = club.getLead();
                if (lead != null && lead.getRole().isClubLead()) {
                    Role expectedRole = Role.forClubCategory(club.getCategory());
                    if (lead.getRole() != expectedRole) {
                        lead.setRole(expectedRole);
                        userRepository.save(lead);
                    }
                }
            }
            return;
        }

        User admin = new User(
                "Dr. S. K. Kulkarni",
                "admin@vpmcoe.ac.in",
                passwordEncoder.encode("admin123"),
                "FAC-ADM-01",
                "Dean Student Affairs",
                "Faculty",
                "+91 98220 11223",
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);

        User techLead = new User(
                "Aarav Deshmukh",
                "techlead@vpmcoe.ac.in",
                passwordEncoder.encode("tech123"),
                "PRN2024COMP045",
                "Computer Engineering",
                "Third Year (TE)",
                "+91 98901 23456",
                Role.ROLE_TECH_CLUB_LEAD
        );
        userRepository.save(techLead);

        User cultLead = new User(
                "Sneha Patil",
                "cultlead@vpmcoe.ac.in",
                passwordEncoder.encode("cult123"),
                "PRN2024AIDS019",
                "AI & Data Science",
                "Third Year (TE)",
                "+91 97654 32109",
                Role.ROLE_CULT_CLUB_LEAD
        );
        userRepository.save(cultLead);

        User sportsLead = new User(
                "Vikram Joshi",
                "sportslead@vpmcoe.ac.in",
                passwordEncoder.encode("sports123"),
                "PRN2023MECH088",
                "Mechanical Engineering",
                "Final Year (BE)",
                "+91 94231 45678",
                Role.ROLE_SPORTS_CLUB_LEAD
        );
        userRepository.save(sportsLead);

        User researchLead = new User(
                "Dr. Pooja Shinde",
                "researchlead@vpmcoe.ac.in",
                passwordEncoder.encode("research123"),
                "FAC-RES-07",
                "Information Technology",
                "Faculty",
                "+91 98233 77889",
                Role.ROLE_RESEARCH_CLUB_LEAD
        );
        userRepository.save(researchLead);

        User student1 = new User(
                "Rohan Kadam",
                "student@vpmcoe.ac.in",
                passwordEncoder.encode("student123"),
                "PRN2025IT034",
                "Information Technology",
                "Second Year (SE)",
                "+91 91580 12345",
                Role.ROLE_STUDENT
        );
        userRepository.save(student1);

        User student2 = new User(
                "Ananya More",
                "ananya.more@vpmcoe.ac.in",
                passwordEncoder.encode("student123"),
                "PRN2024COMP012",
                "Computer Engineering",
                "Third Year (TE)",
                "+91 98811 54321",
                Role.ROLE_STUDENT
        );
        userRepository.save(student2);

        User student3 = new User(
                "Kartik Patel",
                "kartik.patel@vpmcoe.ac.in",
                passwordEncoder.encode("student123"),
                "PRN2026ENTC099",
                "Electronics & Telecommunication",
                "First Year (FE)",
                "+91 96041 98765",
                Role.ROLE_STUDENT
        );
        userRepository.save(student3);

        Club techClub = new Club(
                "Technology Club",
                ClubCategory.TECHNICAL,
                "Innovating the future with code, algorithms, and cutting-edge engineering.",
                "The Technology Club of VPMCOE is the epicenter of software development, artificial intelligence, competitive programming, and robotics on campus. We bridge the gap between academic theory and real-world tech through hackathons, open-source projects, and collaborative peer learning.",
                techLead,
                "Prof. S. R. Jagtap (Computer Dept)",
                "Central Computing Lab 4, IT Complex",
                "Every Wednesday at 4:30 PM",
                "techclub@vpmcoe.ac.in",
                "linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)"
        );
        clubRepository.save(techClub);

        Club cultClub = new Club(
                "Cultural Club",
                ClubCategory.CULTURAL,
                "Celebrating artistic expressions, theater, rhythm, and vibrant campus life.",
                "Cultural Club brings color, excitement, and harmony to VPMCOE.",
                cultLead,
                "Prof. N. V. Gaikwad",
                "College Main Auditorium & Amphitheatre",
                "Every Friday at 5:00 PM",
                "cultural@vpmcoe.ac.in",
                "linear-gradient(135deg, #eb3349 0%, #f45c43 100%)"
        );
        clubRepository.save(cultClub);

        Club sportsClub = new Club(
                "Sports & Athletics Club",
                ClubCategory.SPORTS,
                "Fostering athletic resilience, teamwork, and championship sportsmanship.",
                "VPMCOE Sports Club drives collegiate sports excellence in Cricket, Football, Basketball, Badminton, Chess, and Track & Field.",
                sportsLead,
                "Dr. R. M. Bhosale",
                "Gymkhana Complex & Sports Grounds",
                "Daily Morning 6:00 AM & Evening 5:00 PM",
                "sports@vpmcoe.ac.in",
                "linear-gradient(135deg, #11998e 0%, #38ef7d 100%)"
        );
        clubRepository.save(sportsClub);

        Club researchClub = new Club(
                "Research & Innovation Club",
                ClubCategory.RESEARCH,
                "Empowering inquisitive minds into published researchers and patent inventors.",
                "The Research & Innovation Club is dedicated to mentoring undergraduate students in writing peer-reviewed research papers.",
                researchLead,
                "Dr. V. B. Jadhav",
                "R&D Incubation Center, Room 201",
                "Every Alternate Saturday at 11:00 AM",
                "research@vpmcoe.ac.in",
                "linear-gradient(135deg, #8a2387 0%, #e94057 50%, #f27121 100%)"
        );
        clubRepository.save(researchClub);

        Club literaryClub = new Club(
                "Literary & Debating Society",
                ClubCategory.LITERARY,
                "The campus stage for public rhetoric, debate, and intellectual discourse.",
                "VPMCOE Literary Club sharpens student communication through Model United Nations, debates, poetry slams, and creative writing.",
                null,
                "Prof. M. S. Pawar",
                "Seminar Hall 2, Main Building",
                "Every Thursday at 4:00 PM",
                "literary@vpmcoe.ac.in",
                "linear-gradient(135deg, #f7971e 0%, #ffd200 100%)"
        );
        clubRepository.save(literaryClub);

        Event hackathon = new Event(
                "HackVPM 2026: 24-Hour Annual Hackathon",
                "Join 200+ top student developers in building game-changing solutions for Smart Cities, Healthcare AI, and Sustainable Tech.",
                techClub,
                LocalDateTime.now().plusDays(10).withHour(9).withMinute(0),
                LocalDateTime.now().plusDays(11).withHour(11).withMinute(0),
                "Central Computing Labs & Seminar Hall 1",
                EventMode.OFFLINE,
                null,
                LocalDateTime.now().plusDays(8),
                120,
                "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800&q=80"
        );
        eventRepository.save(hackathon);

        Event aarohan = new Event(
                "Aarohan 2026: Annual Cultural Auditions & Music Night",
                "Open auditions for the official college band, dance squad, and theatre teams.",
                cultClub,
                LocalDateTime.now().plusDays(5).withHour(17).withMinute(30),
                LocalDateTime.now().plusDays(5).withHour(21).withMinute(0),
                "Main College Amphitheatre",
                EventMode.OFFLINE,
                null,
                LocalDateTime.now().plusDays(4),
                250,
                "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800&q=80"
        );
        eventRepository.save(aarohan);

        Event cricket = new Event(
                "Inter-Department Cricket Championship 2026",
                "The most anticipated sporting tournament of the semester!",
                sportsClub,
                LocalDateTime.now().plusDays(15).withHour(8).withMinute(0),
                LocalDateTime.now().plusDays(18).withHour(18).withMinute(0),
                "VPMCOE Sports Ground",
                EventMode.OFFLINE,
                null,
                LocalDateTime.now().plusDays(12),
                300,
                "https://images.unsplash.com/photo-1531415074868-036b1c57e329?w=800&q=80"
        );
        eventRepository.save(cricket);

        Event aiBootcamp = new Event(
                "Hands-on Workshop: Generative AI & Full-Stack Spring Boot",
                "A practical hands-on session on integrating LLM APIs, Vector Databases, and Spring Boot 3 backends.",
                techClub,
                LocalDateTime.now().plusDays(3).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(17).withMinute(30),
                "Lab 304, IT Building & Google Meet",
                EventMode.HYBRID,
                "https://meet.google.com/abc-vpm-xyz",
                LocalDateTime.now().plusDays(2),
                80,
                "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80"
        );
        eventRepository.save(aiBootcamp);

        ClubMembership m1 = new ClubMembership(
                techClub, student1,
                MembershipStatus.APPROVED,
                ClubRole.CORE_MEMBER,
                "Passionate about Web Development and Spring Boot.",
                "Java, Spring Boot, MySQL, HTML/CSS, Git"
        );
        m1.setReviewedAt(LocalDateTime.now().minusDays(2));
        m1.setReviewNotes("Strong backend skills. Approved as Core Technical Member.");
        membershipRepository.save(m1);

        ClubMembership m2 = new ClubMembership(
                cultClub, student1,
                MembershipStatus.PENDING,
                ClubRole.GENERAL_MEMBER,
                "Have played acoustic guitar for 4 years.",
                "Acoustic Guitar, Rhythm, Stage Performance"
        );
        membershipRepository.save(m2);

        ClubMembership m3 = new ClubMembership(
                techClub, student2,
                MembershipStatus.APPROVED,
                ClubRole.GENERAL_MEMBER,
                "Eager to contribute to hackathons and frontend development.",
                "React, UI/UX, Figma, Python"
        );
        m3.setReviewedAt(LocalDateTime.now().minusDays(3));
        membershipRepository.save(m3);

        registrationRepository.save(
                new EventRegistration(hackathon, student1, "VPM-HACK-8492")
        );

        registrationRepository.save(
                new EventRegistration(aiBootcamp, student1, "VPM-AI-3104")
        );

        registrationRepository.save(
                new EventRegistration(hackathon, student2, "VPM-HACK-6721")
        );

        registrationRepository.save(
                new EventRegistration(aarohan, student2, "VPM-AAROH-9021")
        );

        Announcement a1 = new Announcement(
                "Welcome to VPMCOE ClubConnect Portal 2026-27!",
                "All engineering students are invited to explore clubs, apply for memberships, and register for upcoming events.",
                null,
                AnnouncementPriority.URGENT,
                admin
        );
        announcementRepository.save(a1);

        Announcement a2 = new Announcement(
                "HackVPM 2026 Problem Statements Released!",
                "Teams registered for HackVPM 2026 can now review the official tracks.",
                techClub,
                AnnouncementPriority.NORMAL,
                techLead
        );
        announcementRepository.save(a2);

        Announcement a3 = new Announcement(
                "Aarohan 2026 Audition Schedule & Guidelines",
                "Instrumentalists and vocalists should prepare a 3-minute performance.",
                cultClub,
                AnnouncementPriority.NORMAL,
                cultLead
        );
        announcementRepository.save(a3);
    }
}
