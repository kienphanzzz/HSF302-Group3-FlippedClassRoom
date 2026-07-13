package com.example.fcms.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("FCMS Password Reset OTP");
        message.setText("""
                Your FCMS password reset OTP is: %s

                This code will expire in 5 minutes.
                If you did not request this, please ignore this email.
                """.formatted(otp));

        mailSender.send(message);
    }
}
