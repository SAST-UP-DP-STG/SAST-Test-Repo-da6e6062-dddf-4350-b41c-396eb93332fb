package com.example.userapp.controller;

import com.example.userapp.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.userapp.security.UserPrincipal;

@Controller
public class DashboardController {

    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("currentUser", principal.getUsername());
        model.addAttribute("currentRole", principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "ADMIN" : "USER");
        model.addAttribute("users", userService.findAll());
        return "dashboard";
    }
}
