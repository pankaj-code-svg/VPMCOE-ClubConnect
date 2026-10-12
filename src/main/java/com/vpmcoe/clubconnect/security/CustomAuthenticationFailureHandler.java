package com.vpmcoe.clubconnect.security;

import com.vpmcoe.clubconnect.model.RegistrationStatus;
import com.vpmcoe.clubconnect.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String username = request.getParameter("username");
        String reason = "error";

        if (username != null) {
            reason = userRepository.findByEmail(username.trim().toLowerCase())
                    .map(user -> user.getRegistrationStatus() == RegistrationStatus.PENDING
                            ? "pending"
                            : !user.isEnabled() ? "disabled" : "error")
                    .orElse("error");
        }

        response.sendRedirect(request.getContextPath() + "/login?reason=" + reason);
    }
}
