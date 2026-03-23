package com.shieldapi.shieldapi.service;

import com.shieldapi.shieldapi.dto.AuthResponse;
import com.shieldapi.shieldapi.dto.LoginRequest;
import com.shieldapi.shieldapi.dto.MessageResponse;
import com.shieldapi.shieldapi.dto.RegisterRequest;
import com.shieldapi.shieldapi.entity.AppUser;
import com.shieldapi.shieldapi.exception.ResourceConflictException;
import com.shieldapi.shieldapi.repository.UserRepository;
import com.shieldapi.shieldapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public MessageResponse register(RegisterRequest request) {
        String normalizedUsername = request.username().trim();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResourceConflictException("Username already exists");
        }

        AppUser user = new AppUser(
                normalizedUsername,
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);
        return new MessageResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String username = authentication.getName();
        String token = jwtService.generateToken(username);

        return new AuthResponse(token, "Bearer", username, jwtService.getExpirationMs());
    }
}
