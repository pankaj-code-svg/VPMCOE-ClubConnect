package com.vpmcoe.clubconnect.controller;

import com.vpmcoe.clubconnect.model.User;
import com.vpmcoe.clubconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {

        if ("pending".equals(reason)) {
            model.addAttribute(
                    "errorMessage",
                    "Your registration is waiting for administrator approval."
            );
        } else if ("disabled".equals(reason)) {
            model.addAttribute(
                    "errorMessage",
                    "Your account is currently disabled. Please contact an administrator."
            );
        } else if (error != null) {
            model.addAttribute(
                    "errorMessage",
                    "Invalid email or password. Please try again."
            );
        }

        if (logout != null) {
            model.addAttribute(
                    "successMessage",
                    "You have been logged out successfully."
            );
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {

        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }

        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(
            @ModelAttribute("user") @Valid User user,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerStudent(user);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Registration completed successfully! Your account is waiting for administrator approval. You will be able to log in after your registration is approved."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "auth/register";
        }
    }
}
