package pl.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private static final String SALT = "karent-v1";

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) rawPassword = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String rawPassword, String storedHashOrLegacyPlain) {
        if (storedHashOrLegacyPlain == null) return false;
        if (storedHashOrLegacyPlain.equals(rawPassword)) return true;
        return storedHashOrLegacyPlain.equals(hash(rawPassword));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b) & 0xF, 16));
        }
        return sb.toString();
    }
}
