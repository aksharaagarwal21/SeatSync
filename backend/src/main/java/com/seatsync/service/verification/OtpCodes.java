package com.seatsync.service.verification;

import com.seatsync.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates 6-digit codes and stores only their HMAC-SHA256. A keyed hash (rather than a plain
 * hash) means a leaked database table can't be brute-forced offline across the 10⁶ possible codes.
 */
@Component
public class OtpCodes {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public OtpCodes(JwtProperties jwtProperties) {
        // Domain-separated from JWT signing so the two uses can never collide.
        byte[] material = sha256(("seatsync-otp:" + jwtProperties.secret()).getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(material, ALGORITHM);
    }

    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    /** Constant-time comparison so response timing reveals nothing about the stored hash. */
    public boolean matches(String code, String storedHash) {
        return MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.charAt(at - 1) + email.substring(at);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
