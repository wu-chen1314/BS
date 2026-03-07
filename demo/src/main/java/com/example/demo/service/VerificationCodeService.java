package com.example.demo.service;

import com.example.demo.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerificationCodeService {

    private static final int CODE_LENGTH = 6;
    private static final long EXPIRE_MINUTES = 5;

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    public Result<String> sendCode(String email) {
        if (!StringUtils.hasText(email)) {
            return Result.error("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String code = generateCode();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(normalizedEmail);
            message.setSubject("Verification Code");
            message.setText("Your verification code is " + code + ". It expires in " + EXPIRE_MINUTES + " minutes.");
            mailSender.send(message);
        } catch (Exception ex) {
            return Result.error("Failed to send verification code");
        }

        CodeEntry entry = new CodeEntry(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        codeStore.put(normalizedEmail, entry);
        return Result.success("Verification code sent");
    }

    public boolean verifyCode(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        CodeEntry entry = codeStore.get(normalizedEmail);
        if (entry == null) {
            return false;
        }
        if (entry.expireAt.isBefore(LocalDateTime.now())) {
            codeStore.remove(normalizedEmail);
            return false;
        }
        if (!entry.code.equals(code.trim())) {
            return false;
        }

        codeStore.remove(normalizedEmail);
        return true;
    }

    private String generateCode() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    private static class CodeEntry {
        private final String code;
        private final LocalDateTime expireAt;

        private CodeEntry(String code, LocalDateTime expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}