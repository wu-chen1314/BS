package com.example.demo.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GeoSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public GeoSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        ensureColumn("ich_project", "longitude", "ALTER TABLE ich_project ADD COLUMN longitude DECIMAL(10,6) NULL");
        ensureColumn("ich_project", "latitude", "ALTER TABLE ich_project ADD COLUMN latitude DECIMAL(10,6) NULL");
        ensureColumn("ich_project", "address", "ALTER TABLE ich_project ADD COLUMN address VARCHAR(255) NULL");
        ensureColumn("ich_project", "contact_phone", "ALTER TABLE ich_project ADD COLUMN contact_phone VARCHAR(64) NULL");
        ensureColumn("ich_project", "opening_hours", "ALTER TABLE ich_project ADD COLUMN opening_hours VARCHAR(255) NULL");

        ensureColumn("ich_region", "longitude", "ALTER TABLE ich_region ADD COLUMN longitude DECIMAL(10,6) NULL");
        ensureColumn("ich_region", "latitude", "ALTER TABLE ich_region ADD COLUMN latitude DECIMAL(10,6) NULL");
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        if (!tableExists(tableName)) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
