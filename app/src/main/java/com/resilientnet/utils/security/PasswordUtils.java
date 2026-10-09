package com.resilientnet.utils.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    private PasswordUtils() {
        // Prevent instantiation
    }

    /**
     * Hashes a password using SHA-256.
     */
    public static String hashPassword(String password) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to hash password.", e);
        }
    }

    /**
     * Verifies a plain password against a stored hash.
     */
    public static boolean verifyPassword(String password,
                                         String storedHash) {

        return hashPassword(password).equals(storedHash);
    }
}