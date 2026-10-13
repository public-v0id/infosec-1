package ru.se.ifmo.infosec.lab1.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataController {

    @GetMapping("/data")
    public Map<String, Object> getData(
            Authentication authentication) {

        return Map.of(
                "message", "Protected data",
                "user", authentication.getName()
        );
    }
}