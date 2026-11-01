package com.example.userapp.controller;

import com.example.userapp.service.PasswordResetService;
import com.example.userapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/admin/users/{id}/password-reset/request")
    public String requestReset(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return userService.findById(id).map(user -> {
            String token = passwordResetService.createResetToken(user);
            redirectAttributes.addFlashAttribute("message", "Reset token created.");
            redirectAttributes.addFlashAttribute("resetToken", token);
            redirectAttributes.addFlashAttribute("resetUserId", id);
            return "redirect:/dashboard";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/dashboard";
        });
    }

    @GetMapping("/reset-password-confirm")
    public String resetConfirmPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token != null ? token : "");
        return "reset-password-confirm";
    }

    @PostMapping("/password-reset/confirm")
    public String confirmReset(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Token and new password are required.");
            redirectAttributes.addFlashAttribute("token", token != null ? token : "");
            return "redirect:/reset-password-confirm";
        }
        try {
            if (passwordResetService.confirmReset(token.trim(), newPassword)) {
                redirectAttributes.addFlashAttribute("message", "Password updated. You can sign in with your new password.");
                return "redirect:/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to reset password. Please request a new token and try again.");
            redirectAttributes.addFlashAttribute("token", token);
            return "redirect:/reset-password-confirm";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid or expired token.");
        redirectAttributes.addFlashAttribute("token", token);
        return "redirect:/reset-password-confirm";
    }
}
