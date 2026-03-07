package com.example.demo;

import com.example.demo.common.Result;
import com.example.demo.service.VerificationCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@JdbcTest
@Import(VerificationCodeService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:verification;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-auth-code"
})
class VerificationCodeServiceTest {

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void sendCodePersistsRecordAndConsumesOnce() {
        Result<Boolean> result = verificationCodeService.sendCode("User@Test.com");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));

        String code = jdbcTemplate.queryForObject(
                "SELECT code FROM verification_code WHERE email = ? ORDER BY sent_at DESC LIMIT 1",
                String.class,
                "user@test.com");

        Boolean usedBefore = jdbcTemplate.queryForObject(
                "SELECT used FROM verification_code WHERE email = ? ORDER BY sent_at DESC LIMIT 1",
                Boolean.class,
                "user@test.com");
        assertThat(usedBefore).isFalse();

        assertThat(verificationCodeService.verifyCode("user@test.com", code, true)).isTrue();

        Boolean usedAfter = jdbcTemplate.queryForObject(
                "SELECT used FROM verification_code WHERE email = ? ORDER BY sent_at DESC LIMIT 1",
                Boolean.class,
                "user@test.com");
        assertThat(usedAfter).isTrue();
        assertThat(verificationCodeService.verifyCode("user@test.com", code, true)).isFalse();
    }

    @Test
    void sendCodeRejectsInvalidEmail() {
        Result<Boolean> result = verificationCodeService.sendCode("bad-email");

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM verification_code", Integer.class)).isZero();
    }
}
