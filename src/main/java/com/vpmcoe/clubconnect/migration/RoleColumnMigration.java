package com.vpmcoe.clubconnect.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Ensures older MySQL schemas can store the expanded role names. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoleColumnMigration implements ApplicationRunner {

    private static final int ROLE_COLUMN_LENGTH = 64;

    private final JdbcTemplate jdbcTemplate;

    public RoleColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String databaseProduct = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
        if (databaseProduct == null || !databaseProduct.toLowerCase(Locale.ROOT).contains("mysql")) {
            return;
        }

        jdbcTemplate.query(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH "
                        + "FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'",
                (ResultSetExtractor<Void>) resultSet -> {
                    if (resultSet.next()) {
                        String dataType = resultSet.getString("DATA_TYPE");
                        long columnLength = resultSet.getLong("CHARACTER_MAXIMUM_LENGTH");
                        if ("enum".equalsIgnoreCase(dataType) || columnLength < ROLE_COLUMN_LENGTH) {
                            jdbcTemplate.execute(
                                    "ALTER TABLE `users` MODIFY COLUMN `role` VARCHAR("
                                            + ROLE_COLUMN_LENGTH + ") NOT NULL");
                        }
                    }
                    return null;
                }
        );
    }
}
