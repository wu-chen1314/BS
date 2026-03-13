package com.example.demo;

import com.example.demo.service.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void fifthFailedAttemptLocksAccountImmediately() {
        LoginAttemptService service = new LoginAttemptService();

        for (int index = 0; index < 5; index += 1) {
            service.incrementLoginAttempts("tester");
        }

        assertThat(service.getLoginAttempts("tester")).isEqualTo(5);
        assertThat(service.getRemainingAttempts("tester")).isZero();
        assertThat(service.isAccountLocked("tester")).isTrue();
        assertThat(service.getLockRemainingTime("tester")).isGreaterThan(0);
    }
}
