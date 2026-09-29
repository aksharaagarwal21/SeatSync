package com.seatsync.service;

import com.seatsync.dto.auth.LoginRequest;
import com.seatsync.dto.auth.LoginResponse;
import com.seatsync.dto.auth.RegisterRequest;
import com.seatsync.dto.auth.UserResponse;
import com.seatsync.entity.Role;
import com.seatsync.entity.User;
import com.seatsync.exception.EmailAlreadyRegisteredException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.mapper.UserMapper;
import com.seatsync.repository.UserRepository;
import com.seatsync.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final Clock clock;
    /** Compared against when the email is unknown so response time doesn't reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("seatsync-timing-guard");
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER, clock.instant());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalize(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return issueToken(user.get());
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Your account no longer exists."));
    }

    private LoginResponse issueToken(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), userMapper.toResponse(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
