package com.vpmcoe.clubconnect.config;

import com.vpmcoe.clubconnect.model.Role;
import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "clubconnect.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${clubconnect.bootstrap-admin.name:}") String name,
            @Value("${clubconnect.bootstrap-admin.email:}") String email,
            @Value("${clubconnect.bootstrap-admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.ROLE_ADMIN) > 0) {
            return;
        }

        if (name.isBlank() || email.isBlank() || password.length() < 12) {
            throw new IllegalStateException(
                    "Set BOOTSTRAP_ADMIN_NAME, BOOTSTRAP_ADMIN_EMAIL, and a BOOTSTRAP_ADMIN_PASSWORD "
                            + "of at least 12 characters before starting production."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_EMAIL already belongs to a non-admin account. Choose another email."
            );
        }

        User admin = new User(
                name.trim(), email.trim(), passwordEncoder.encode(password),
                null, null, null, null, Role.ROLE_ADMIN
        );
        userRepository.save(admin);
    }
}
