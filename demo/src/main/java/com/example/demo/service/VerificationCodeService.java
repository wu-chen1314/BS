package com.example.demo.service;

import com.example.demo.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class VerificationCodeService {

    private static final long EXPIRE_MINUTES = 5;
    private static final long RESEND_INTERVAL_SECONDS = 60;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    @Value("${spring.mail.password:}")
    private String senderPassword;

    @PostConstruct
    public void initializeTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS verification_code (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "email VARCHAR(255) NOT NULL," +
                        "code VARCHAR(16) NOT NULL," +
                        "expire_at TIMESTAMP NOT NULL," +
                        "sent_at TIMESTAMP NOT NULL," +
                        "used BOOLEAN NOT NULL DEFAULT FALSE," +
                        "used_at TIMESTAMP NULL," +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ")");
        createIndexIfMissing("idx_verification_code_email",
                "CREATE INDEX idx_verification_code_email ON verification_code(email)");
        createIndexIfMissing("idx_verification_code_expire_at",
                "CREATE INDEX idx_verification_code_expire_at ON verification_code(expire_at)");
        createIndexIfMissing("idx_verification_code_email_used",
                "CREATE INDEX idx_verification_code_email_used ON verification_code(email, used)");
    }

    public Result<Boolean> sendCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail) || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            return Result.error("邮箱格式不正确");
        }
        if (!isMailConfigured()) {
            return Result.error("Mail service is not configured");
        }

        cleanupExpiredCodes();
        VerificationCodeEntry latestEntry = findLatestUnusedCode(normalizedEmail);
        if (latestEntry != null && latestEntry.getSentAt() != null &&
                latestEntry.getSentAt().plusSeconds(RESEND_INTERVAL_SECONDS).isAfter(LocalDateTime.now())) {
            return Result.error("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderAddress);
            message.setTo(normalizedEmail);
            message.setSubject("非遗传承平台验证码");
            message.setText("您的验证码为：" + code + "，5 分钟内有效。如非本人操作，请忽略此邮件。");
            mailSender.send(message);
        } catch (Exception exception) {
            return Result.error("Failed to send verification code");
        }

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO verification_code (email, code, expire_at, sent_at, used) VALUES (?, ?, ?, ?, ?)",
                normalizedEmail,
                code,
                Timestamp.valueOf(now.plusMinutes(EXPIRE_MINUTES)),
                Timestamp.valueOf(now),
                false);
        return Result.success(true);
    }

    public boolean verifyCode(String email, String code, boolean consumeAfterVerification) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail) || !StringUtils.hasText(code)) {
            return false;
        }

        cleanupExpiredCodes();
        List<VerificationCodeEntry> matchedEntries = jdbcTemplate.query(
                "SELECT id, email, code, expire_at, sent_at, used FROM verification_code " +
                        "WHERE email = ? AND code = ? AND used = FALSE ORDER BY sent_at DESC LIMIT 1",
                (rs, rowNum) -> mapEntry(rs),
                normalizedEmail,
                code.trim());

        if (matchedEntries.isEmpty()) {
            return false;
        }

        VerificationCodeEntry entry = matchedEntries.get(0);
        if (entry.getExpireAt() == null || entry.getExpireAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (consumeAfterVerification) {
            jdbcTemplate.update(
                    "UPDATE verification_code SET used = TRUE, used_at = ?, updated_at = ? WHERE id = ?",
                    Timestamp.valueOf(LocalDateTime.now()),
                    Timestamp.valueOf(LocalDateTime.now()),
                    entry.getId());
        }
        return true;
    }

    private VerificationCodeEntry findLatestUnusedCode(String email) {
        List<VerificationCodeEntry> entries = jdbcTemplate.query(
                "SELECT id, email, code, expire_at, sent_at, used FROM verification_code " +
                        "WHERE email = ? AND used = FALSE ORDER BY sent_at DESC LIMIT 1",
                (rs, rowNum) -> mapEntry(rs),
                email);
        return entries.isEmpty() ? null : entries.get(0);
    }

    private void cleanupExpiredCodes() {
        jdbcTemplate.update(
                "DELETE FROM verification_code " +
                        "WHERE expire_at < ? OR (used = TRUE AND updated_at < ?)",
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().minusDays(7)));
    }

    private void createIndexIfMissing(String indexName, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore duplicate index creation across repeated startups.
        }
    }

    private boolean isMailConfigured() {
        if (mailSender == null) {
            return false;
        }
        if (!StringUtils.hasText(senderAddress) || !StringUtils.hasText(senderPassword)) {
            return false;
        }
        return !senderAddress.contains("your-qq-email@example.com")
                && !senderPassword.contains("your-mail-auth-code");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private VerificationCodeEntry mapEntry(ResultSet resultSet) throws SQLException {
        VerificationCodeEntry entry = new VerificationCodeEntry();
        entry.setId(resultSet.getLong("id"));
        entry.setEmail(resultSet.getString("email"));
        entry.setCode(resultSet.getString("code"));
        Timestamp expireAt = resultSet.getTimestamp("expire_at");
        Timestamp sentAt = resultSet.getTimestamp("sent_at");
        entry.setExpireAt(expireAt == null ? null : expireAt.toLocalDateTime());
        entry.setSentAt(sentAt == null ? null : sentAt.toLocalDateTime());
        entry.setUsed(resultSet.getBoolean("used"));
        return entry;
    }

    private static class VerificationCodeEntry {
        private Long id;
        private String email;
        private String code;
        private LocalDateTime expireAt;
        private LocalDateTime sentAt;
        private boolean used;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }

        public void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }

        public boolean isUsed() {
            return used;
        }

        public void setUsed(boolean used) {
            this.used = used;
        }
    }
}
