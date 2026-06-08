package com.idea.controller;

import com.idea.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Hello World controller — demonstrates a live DB read.
 *
 * GET /hello        → public, no auth needed
 * GET /hello/me     → protected, requires Bearer token
 */
@RestController
@RequestMapping("/hello")
@RequiredArgsConstructor
public class HelloController {

    private final UserRepository userRepository;

    /** Public endpoint — hits the DB to confirm connectivity. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> hello() {
        long userCount = userRepository.count();   // single DB call
        return ResponseEntity.ok(Map.of(
            "message",   "idea-backend is running",
            "timestamp", Instant.now(),
            "userCount", userCount
        ));
    }

    /** Protected endpoint — requires a valid JWT. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
        @org.springframework.security.core.annotation.AuthenticationPrincipal
        org.springframework.security.core.userdetails.UserDetails principal
    ) {
        return ResponseEntity.ok(Map.of(
            "email", principal.getUsername(),
            "roles", principal.getAuthorities()
        ));
    }
}
