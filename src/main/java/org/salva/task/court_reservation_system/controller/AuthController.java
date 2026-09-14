package org.salva.task.court_reservation_system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salva.task.court_reservation_system.dto.request.AuthRequestDTO;
import org.salva.task.court_reservation_system.dto.request.UserRequestDTO;
import org.salva.task.court_reservation_system.dto.response.AuthResponseDTO;
import org.salva.task.court_reservation_system.entity.User;
import org.salva.task.court_reservation_system.enums.Role;
import org.salva.task.court_reservation_system.exception.ValidationException;
import org.salva.task.court_reservation_system.mapper.UserMapper;
import org.salva.task.court_reservation_system.repository.UserRepository;
import org.salva.task.court_reservation_system.security.CustomUserDetails;
import org.salva.task.court_reservation_system.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("El email ya está en uso");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(jwtToken)
                .name(user.getName())
                .role(user.getRole().name())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwtToken = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(AuthResponseDTO.builder()
                .token(jwtToken)
                .name(userDetails.getUser().getName())
                .role(userDetails.getUser().getRole().name())
                .build());
    }
}
