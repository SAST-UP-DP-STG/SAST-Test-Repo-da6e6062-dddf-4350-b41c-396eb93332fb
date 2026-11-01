package com.example.userapp.controller;

import com.example.userapp.entity.UserRole;
import com.example.userapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public String createUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "USER") String role,
            @RequestParam(defaultValue = "true") boolean enabled,
            RedirectAttributes redirectAttributes) {
        if (userService.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already exists.");
            return "redirect:/dashboard";
        }
        UserRole r = "ADMIN".equalsIgnoreCase(role) ? UserRole.ADMIN : UserRole.USER;
        userService.createUser(email, password, r, enabled);
        redirectAttributes.addFlashAttribute("message", "User created.");
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/edit")
    public String editUser(
            @PathVariable Long id,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            RedirectAttributes redirectAttributes) {
        UserRole r = null;
        if (role != null && !role.isBlank()) {
            r = "ADMIN".equalsIgnoreCase(role) ? UserRole.ADMIN : UserRole.USER;
        }
        if (userService.updateUser(id, email, r, enabled).isPresent()) {
            redirectAttributes.addFlashAttribute("message", "User updated.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (userService.deleteUser(id)) {
            redirectAttributes.addFlashAttribute("message", "User deleted.");
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        return "redirect:/dashboard";
    }
}
