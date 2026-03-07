package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:context-load;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.redis.enabled=false",
                "spring.mail.username=test@example.com",
                "spring.mail.password=test-auth-code"
        })
class DemoApplicationTests {

    @MockBean
    private ServerEndpointExporter serverEndpointExporter;

    @Test
    void contextLoads() {
    }
}
