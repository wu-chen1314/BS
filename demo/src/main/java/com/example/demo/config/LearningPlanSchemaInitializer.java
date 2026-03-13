package com.example.demo.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class LearningPlanSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public LearningPlanSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learning_plan_favorite (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "plan_id VARCHAR(96) NOT NULL," +
                "plan_title VARCHAR(160) NULL," +
                "track_id VARCHAR(64) NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_learning_plan_user_plan (user_id, plan_id)," +
                "KEY idx_learning_plan_favorite_user_created (user_id, created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learning_plan_analytics (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NULL," +
                "plan_id VARCHAR(96) NOT NULL," +
                "plan_title VARCHAR(160) NULL," +
                "track_id VARCHAR(64) NULL," +
                "action_type VARCHAR(32) NOT NULL," +
                "audience_tag VARCHAR(128) NULL," +
                "duration_label VARCHAR(64) NULL," +
                "linked_theme_id VARCHAR(64) NULL," +
                "region_keyword VARCHAR(128) NULL," +
                "project_count INT NULL DEFAULT 0," +
                "keyword_tags VARCHAR(255) NULL," +
                "payload_json TEXT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_learning_plan_analytics_created (created_at)," +
                "KEY idx_learning_plan_analytics_plan_action (plan_id, action_type)," +
                "KEY idx_learning_plan_analytics_track (track_id)," +
                "KEY idx_learning_plan_analytics_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
}
