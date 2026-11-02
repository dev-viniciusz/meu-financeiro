package com.meufinanceiro.controller;

import com.meufinanceiro.dto.Requests;
import com.meufinanceiro.model.Profile;
import com.meufinanceiro.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserService service;

    public ProfileController(UserService service) {
        this.service = service;
    }

    // "userId" é preenchido pelo AuthInterceptor a partir do token de login.
    @GetMapping
    public Profile get(@RequestAttribute("userId") UUID userId) {
        return service.getProfile(userId);
    }

    @PutMapping
    public Profile update(@RequestAttribute("userId") UUID userId, @RequestBody Requests.ProfileUpdate body) {
        return service.updateProfile(userId, body);
    }
}
