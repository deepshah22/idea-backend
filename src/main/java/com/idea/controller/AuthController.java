package com.idea.controller;

import com.idea.dao.UserRepository;
import com.idea.entity.User;
import com.idea.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name
    ) {}

    record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}

    record AuthResponse(String accessToken, String refreshToken, String email) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().build();
        }

        userRepository.save(User.builder()
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .name(req.name())
            .build());

        return ResponseEntity.ok(tokens(req.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        return ResponseEntity.ok(tokens(req.email()));
    }

    private AuthResponse tokens(String email) {
        var details = userDetailsService.loadUserByUsername(email);
        return new AuthResponse(
            jwtService.generateToken(details),
            jwtService.generateRefreshToken(details),
            email
        );
    }
}
