package com.seatsync.service;

import com.seatsync.config.OtpProperties;
import com.seatsync.dto.auth.LoginRequest;
import com.seatsync.dto.auth.LoginResponse;
import com.seatsync.dto.auth.RegisterRequest;
import com.seatsync.dto.auth.UserResponse;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.Role;
import com.seatsync.entity.User;
import com.seatsync.exception.EmailAlreadyRegisteredException;
import com.seatsync.exception.ResourceNotFoundException;
import com.seatsync.mapper.UserMapper;
import com.seatsync.repository.UserRepository;
import com.seatsync.security.JwtService;
import com.seatsync.service.verification.OtpService;
import com.seatsync.service.verification.OtpVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

/**
 * Sign-in and registration. With two-step verification on, a correct password (or a new
 * registration) only emails a code; the session is issued by {@link #verify}.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final OtpVerifier otpVerifier;
    private final OtpProperties otpProperties;
    private final Clock clock;
    /** Compared against when the email is unknown so response time doesn't reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper,
                       OtpService otpService,
                       OtpVerifier otpVerifier,
                       OtpProperties otpProperties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.otpService = otpService;
        this.otpVerifier = otpVerifier;
        this.otpProperties = otpProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("seatsync-timing-guard");
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        String name = request.name().trim();
        String passwordHash = passwordEncoder.encode(request.password());

        User user = userRepository.findByEmail(email)
                .map(existing -> restartUnverified(existing, name, passwordHash))
                .orElseGet(() -> createUser(name, email, passwordHash));

        if (!otpProperties.enabled()) {
            user.markEmailVerified();
            return issueSession(user);
        }
        return LoginResponse.verificationRequired(
                otpService.issue(user, OtpPurpose.REGISTRATION, null, "Create your SeatSync account"));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(normalize(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        if (!otpProperties.enabled()) {
            return issueSession(user.get());
        }
        return LoginResponse.verificationRequired(
                otpService.issue(user.get(), OtpPurpose.LOGIN, null, "Sign in to SeatSync"));
    }

    /** Second step of sign-in and registration: a valid code proves the email and starts the session. */
    @Transactional
    public LoginResponse verify(VerificationCode code) {
        Long userId = otpVerifier.verify(code, null, EnumSet.of(OtpPurpose.LOGIN, OtpPurpose.REGISTRATION), null);
        otpService.consume(code.challengeId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Your account no longer exists."));
        user.markEmailVerified();
        return issueSession(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Your account no longer exists."));
    }

    /** An unverified sign-up can be taken over by whoever proves the email; verified ones cannot. */
    private User restartUnverified(User existing, String name, String passwordHash) {
        if (existing.isEmailVerified() || !otpProperties.enabled()) {
            throw new EmailAlreadyRegisteredException();
        }
        existing.restartRegistration(name, passwordHash);
        return existing;
    }

    private User createUser(String name, String email, String passwordHash) {
        try {
            return userRepository.saveAndFlush(new User(name, email, passwordHash, Role.USER, clock.instant()));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private LoginResponse issueSession(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return LoginResponse.authenticated(token.value(), token.expiresAt(), userMapper.toResponse(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
