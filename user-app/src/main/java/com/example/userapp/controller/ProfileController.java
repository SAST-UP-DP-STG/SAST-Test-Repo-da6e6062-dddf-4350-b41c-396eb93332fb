package com.example.userapp.controller;

import com.example.userapp.security.UserPrincipal;
import com.example.userapp.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/me")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        Long currentUserId = principal.getId();
        var userOpt = userService.findById(currentUserId);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/dashboard";
        }
        if (email != null && !email.isBlank()) {
            if (userService.findByEmail(email).filter(u -> !u.getId().equals(currentUserId)).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email already in use.");
                return "redirect:/dashboard";
            }
            userService.updateUser(currentUserId, email, null, null);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            userService.setPassword(currentUserId, newPassword);
        }
        redirectAttributes.addFlashAttribute("message", "Profile updated.");
        return "redirect:/dashboard";
    }
}
