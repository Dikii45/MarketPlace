package com.marketHub.marketplace.controllers.rest;

import com.marketHub.marketplace.dto.LoginRequest;
import com.marketHub.marketplace.dto.LoginResponse;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.repositories.UserRepository;
import com.marketHub.marketplace.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
* Разработка закончена 08.09.2026
* Возможно будет возобновлена после разработки андроид приложения
*
* */

// вход в REST API — отдельно от формы /login сайта, отдаёт JWT вместо cookie-сессии
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Неверный email или пароль"));
        }

        if (!user.isActive()) {
            return ResponseEntity.status(403).body(Map.of("error", "Аккаунт заблокирован"));
        }

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getName(), user.getEmail()));
    }
}
