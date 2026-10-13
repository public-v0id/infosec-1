package ru.se.ifmo.infosec.lab1.service;

import ru.se.ifmo.infosec.lab1.dto.LoginRequest;
import ru.se.ifmo.infosec.lab1.dto.LoginResponse;
import ru.se.ifmo.infosec.lab1.entity.User;
import ru.se.ifmo.infosec.lab1.repository.UserRepository;
import ru.se.ifmo.infosec.lab1.exception.InvalidCredentialsException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.se.ifmo.infosec.lab1.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException());

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getUsername());

        return new LoginResponse(token);
    }
}
