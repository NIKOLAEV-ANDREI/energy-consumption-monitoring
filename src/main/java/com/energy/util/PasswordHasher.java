package com.energy.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Утилитарный класс для хэширования паролей
 * Использует алгоритм SHA-256 для безопасного хранения паролей
 */
public class PasswordHasher {
    
    /**
     * Хэширование пароля с использованием SHA-256
     * @param password исходный пароль
     * @return хэш пароля в шестнадцатеричном формате
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хэширования пароля: " + e.getMessage());
        }
    }
    
    /**
     * Проверка соответствия пароля хэшу
     * @param password введённый пароль
     * @param hash сохранённый хэш
     * @return true если пароль соответствует хэшу
     */
    public static boolean verifyPassword(String password, String hash) {
        String passwordHash = hashPassword(password);
        return passwordHash.equals(hash);
    }
    
    /**
     * Преобразование массива байтов в шестнадцатеричную строку
     * @param bytes массив байтов
     * @return строка в hex-формате
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}



