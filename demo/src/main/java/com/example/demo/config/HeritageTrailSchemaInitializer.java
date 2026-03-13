package com.example.demo.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class HeritageTrailSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public HeritageTrailSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS heritage_trail_favorite (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "trail_id VARCHAR(64) NOT NULL," +
                "trail_title VARCHAR(128) NULL," +
                "route_type VARCHAR(32) NOT NULL DEFAULT 'template'," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_trail_favorite_user_route (user_id, trail_id, route_type)," +
                "KEY idx_trail_favorite_user_created (user_id, created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS heritage_trail_analytics (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NULL," +
                "trail_id VARCHAR(64) NOT NULL," +
                "trail_title VARCHAR(128) NULL," +
                "route_type VARCHAR(32) NOT NULL DEFAULT 'template'," +
                "action_type VARCHAR(32) NOT NULL," +
                "transport_mode VARCHAR(32) NULL," +
                "budget_level VARCHAR(32) NULL," +
                "duration_key VARCHAR(32) NULL," +
                "interest_tags VARCHAR(255) NULL," +
                "stop_count INT NULL DEFAULT 0," +
                "estimated_hours DECIMAL(8,2) NULL," +
                "estimated_cost DECIMAL(10,2) NULL," +
                "payload_json TEXT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_trail_analytics_created (created_at)," +
                "KEY idx_trail_analytics_trail_action (trail_id, action_type)," +
                "KEY idx_trail_analytics_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
}
