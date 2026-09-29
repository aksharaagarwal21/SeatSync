package com.seatsync.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Customer-facing booking IDs such as SS-7K3F9QMP. Ambiguous characters (0/O, 1/I) are excluded. */
@Component
public class BookingReferenceGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder reference = new StringBuilder("SS-");
        for (int i = 0; i < LENGTH; i++) {
            reference.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
