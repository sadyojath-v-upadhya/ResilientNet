package com.resilientnet.utils.validation;

import android.util.Patterns;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidationUtils {

    private ValidationUtils() {
        // Prevent instantiation
    }

    // -----------------------------
    // CONSTANTS
    // -----------------------------

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 50;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 32;

    private static final int MAX_EMAIL_LENGTH = 100;

    // Full name
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z]+([ '-][A-Za-z]+)*$");

    // Strict email
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9]+([._][A-Za-z0-9]+)*@[A-Za-z0-9-]+(\\.[A-Za-z]{2,})+$"
            );

    // Indian phone
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");

    // Strong password
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-]).{8,32}$"
            );

    // Weak passwords
    private static final Set<String> COMMON_PASSWORDS =
            new HashSet<>(Arrays.asList(

                    "password",
                    "password123",
                    "12345678",
                    "123456789",
                    "qwerty",
                    "qwerty123",
                    "admin",
                    "admin123",
                    "welcome",
                    "letmein"

            ));

    // -------------------------------------------------------
    // NAME
    // -------------------------------------------------------

    public static String validateName(String name) {

        if (name == null)
            return "Full name is required.";

        name = name.trim();

        if (name.isEmpty())
            return "Full name cannot be empty.";

        if (name.length() < MIN_NAME_LENGTH)
            return "Full name must contain at least 3 characters.";

        if (name.length() > MAX_NAME_LENGTH)
            return "Full name cannot exceed 50 characters.";

        if (!NAME_PATTERN.matcher(name).matches())
            return "Full name can contain only letters, spaces, apostrophes, and hyphens.";

        if (name.contains("  "))
            return "Full name cannot contain multiple consecutive spaces.";

        return null;
    }

    // -------------------------------------------------------
    // EMAIL
    // -------------------------------------------------------

    public static String validateEmail(String email) {

        if (email == null)
            return "Email is required.";

        email = email.trim();

        if (email.isEmpty())
            return "Email cannot be empty.";

        if (email.length() > MAX_EMAIL_LENGTH)
            return "Email is too long.";

        if (email.contains(" "))
            return "Email cannot contain spaces.";

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Enter a valid email address.";

        if (!EMAIL_PATTERN.matcher(email).matches())
            return "Email must be like example@gmail.com.";

        return null;
    }

    // -------------------------------------------------------
    // PHONE
    // -------------------------------------------------------

    public static String validatePhone(String phone) {

        if (phone == null)
            return "Phone number is required.";

        phone = phone.trim();

        if (phone.isEmpty())
            return "Phone number cannot be empty.";

        if (!PHONE_PATTERN.matcher(phone).matches())
            return "Enter a valid 10-digit Indian mobile number.";

        return null;
    }

    // -------------------------------------------------------
    // PASSWORD
    // -------------------------------------------------------

    public static String validatePassword(String password) {

        if (password == null)
            return "Password is required.";

        if (password.isEmpty())
            return "Password cannot be empty.";

        if (password.contains(" "))
            return "Password cannot contain spaces.";

        if (password.length() < MIN_PASSWORD_LENGTH)
            return "Password must be at least 8 characters.";

        if (password.length() > MAX_PASSWORD_LENGTH)
            return "Password cannot exceed 32 characters.";

        if (COMMON_PASSWORDS.contains(password.toLowerCase()))
            return "Choose a stronger password.";

        if (!PASSWORD_PATTERN.matcher(password).matches())
            return "Password must contain uppercase, lowercase, number, and special character.";

        return null;
    }

    // -------------------------------------------------------
    // CONFIRM PASSWORD
    // -------------------------------------------------------

    public static String validateConfirmPassword(
            String password,
            String confirmPassword
    ) {

        if (confirmPassword == null)
            return "Confirm password is required.";

        if (confirmPassword.isEmpty())
            return "Confirm password cannot be empty.";

        if (!password.equals(confirmPassword))
            return "Passwords do not match.";

        return null;
    }

    // -------------------------------------------------------
    // EMERGENCY CONTACT
    // -------------------------------------------------------

    public static String validateEmergencyContact(String contact) {

        return validateName(contact);
    }

}