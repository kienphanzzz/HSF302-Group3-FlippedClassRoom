package com.example.fcms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${fcms.otp.expiry-minutes:5}")
    private int expiryMinutes;

    public String createOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        otpStore.put(email, new OtpEntry(otp, expiresAt));

        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);

        if (entry == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            otpStore.remove(email);
            return false;
        }

        boolean matches = entry.otp().equals(otp);
        if (matches) {
            otpStore.remove(email);
        }

        return matches;
    }

    private record OtpEntry(String otp, LocalDateTime expiresAt) {
    }

}
