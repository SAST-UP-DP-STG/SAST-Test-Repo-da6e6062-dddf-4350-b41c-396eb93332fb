package com.example.userapp.controller;

import com.example.userapp.entity.UserRole;
import com.example.userapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "logout", required = false) String logout, Model model) {
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/auth/register")
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        if (userService.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already registered.");
            return "redirect:/register";
        }
        userService.createUser(email, password, UserRole.USER, true);
        redirectAttributes.addFlashAttribute("message", "Registration successful. Please log in.");
        return "redirect:/login";
    }
}
