package ru.se.ifmo.infosec.lab1.controller;

import jakarta.validation.Valid;
import ru.se.ifmo.infosec.lab1.dto.LoginRequest;
import ru.se.ifmo.infosec.lab1.dto.LoginResponse;
import ru.se.ifmo.infosec.lab1.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}