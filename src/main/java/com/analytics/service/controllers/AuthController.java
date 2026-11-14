package com.analytics.service.controllers;

import com.analytics.service.dto.AuthResponse;
import com.analytics.service.dto.LoginRequest;
import com.analytics.service.dto.RegisterRequest;
import com.analytics.service.entity.User;
import com.analytics.service.repository.UserRepository;
import com.analytics.service.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Вход по email ИЛИ телефону (поле login) + пароль. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверный логин или пароль");
        }

        User user = userRepository.findByEmail(request.login())
                .or(() -> userRepository.findByUserPhone(request.login()))
                .orElseThrow();
        String role = roleOf(user);
        String subject = user.getEmail() != null ? user.getEmail() : user.getUserPhone();
        String token = jwtService.generateToken(subject, role);
        return ResponseEntity.ok(new AuthResponse(token, subject, role));
    }

    /** Регистрация обычного пользователя (роль USER). Админа назначай в БД. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        if (request.userPhone() != null && userRepository.findByUserPhone(request.userPhone()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким телефоном уже существует");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserPhone(request.userPhone());
        user.setRole("USER");
        userRepository.save(user);

        String role = roleOf(user);
        String subject = user.getEmail() != null ? user.getEmail() : user.getUserPhone();
        String token = jwtService.generateToken(subject, role);
        return ResponseEntity.ok(new AuthResponse(token, subject, role));
    }

    private String roleOf(User user) {
        String role = (user.getRole() == null || user.getRole().isBlank()) ? "USER" : user.getRole();
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
