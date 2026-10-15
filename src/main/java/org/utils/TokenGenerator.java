package org.utils;

import java.security.SecureRandom;
import java.util.UUID;

public class TokenGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateToken() {
        StringBuilder stringBuilder = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            stringBuilder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return stringBuilder.toString();
    }

    //TODO: BUG, Token should be accepted in regex A-Z0-9, but now accepting in A-F0-9 regex
    public static String generateValidToken() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
