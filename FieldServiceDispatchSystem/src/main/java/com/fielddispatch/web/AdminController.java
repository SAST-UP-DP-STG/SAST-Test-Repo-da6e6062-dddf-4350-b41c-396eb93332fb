package com.fielddispatch.web;

import com.fielddispatch.domain.UserAccount;
import com.fielddispatch.repo.UserAccountRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final UserAccountRepository userAccountRepository;

    public AdminController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/operators")
    public List<Map<String, Object>> operators() {
        return userAccountRepository.findAll().stream().map(this::toView).toList();
    }

    private Map<String, Object> toView(UserAccount user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("fullName", user.getFullName());
        view.put("role", user.getRole());
        view.put("createdAt", user.getCreatedAt().toString());
        return view;
    }
}
