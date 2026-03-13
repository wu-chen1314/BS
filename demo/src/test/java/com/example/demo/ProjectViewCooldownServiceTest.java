package com.example.demo;

import com.example.demo.service.ProjectViewCooldownService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectViewCooldownServiceTest {

    @Test
    void allowsFirstViewAndBlocksRepeatedViewWithinCooldownWindow() throws Exception {
        ProjectViewCooldownService service = new ProjectViewCooldownService();

        assertThat(service.shouldCountView(1L, "user:7", 1)).isTrue();
        assertThat(service.shouldCountView(1L, "user:7", 1)).isFalse();

        Thread.sleep(1100L);

        assertThat(service.shouldCountView(1L, "user:7", 1)).isTrue();
    }
}
