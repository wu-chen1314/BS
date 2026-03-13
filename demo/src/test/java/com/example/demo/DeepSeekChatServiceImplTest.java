package com.example.demo;

import com.example.demo.service.impl.DeepSeekChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekChatServiceImplTest {

    @Test
    void generateReplyFailsFastWhenApiKeyIsMissing() {
        DeepSeekChatServiceImpl service = new DeepSeekChatServiceImpl();
        ReflectionTestUtils.setField(service, "apiKey", " ");

        assertThatThrownBy(() -> service.generateReply("你好"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI 服务未配置，请先设置 DEEPSEEK_API_KEY");
    }
}
