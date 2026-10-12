
package com.smarttracker.backend.controller;

import com.smarttracker.backend.entity.User;
import com.smarttracker.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.smarttracker.backend.security.JwtService;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:3001"
})
public class AuthController {

    private final UserRepository userRepository;
private final BCryptPasswordEncoder passwordEncoder;
private final JwtService jwtService;

public AuthController(
        UserRepository userRepository,
        BCryptPasswordEncoder passwordEncoder,
        JwtService jwtService) {

    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        if (user.getFullName() == null ||
            user.getFullName().isBlank() ||
            user.getEmail() == null ||
            user.getEmail().isBlank() ||
            user.getPassword() == null ||
            user.getPassword().length() < 8) {

            return ResponseEntity.badRequest()
                .body(Map.of(
                    "message",
                    "Enter your name and email, and a password of at least 8 characters."
                ));
        }

        if (userRepository.existsByEmail(user.getEmail().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Email is already registered."));
        }

        user.setFullName(user.getFullName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Registration successful!",
                "id", savedUser.getId(),
                "fullName", savedUser.getFullName(),
                "email", savedUser.getEmail()
            ));
    }
    
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

    String email = request.get("email");
    String password = request.get("password");

    if (email == null || email.isBlank()
            || password == null || password.isBlank()) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Email and password are required."));
    }

    User user = userRepository
            .findByEmail(email.trim().toLowerCase())
            .orElse(null);

    if (user == null ||
            !passwordEncoder.matches(password, user.getPassword())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password."));
    }

    String token = jwtService.generateToken(user.getEmail());

    return ResponseEntity.ok(Map.of(
            "message", "Login successful!",
            "token", token,
            "fullName", user.getFullName(),
            "email", user.getEmail(),
            "role", user.getRole()
    ));
}
}