package com.vpmcoe.clubconnect.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class H2ToMySqlMigrator {

    private static final List<String> TABLES = List.of(
            "users", "clubs", "announcements", "events", "club_memberships",
            "event_registrations", "email_notification_logs"
    );

    private H2ToMySqlMigrator() {
    }

    public static void main(String[] args) throws Exception {
        String host = setting("MYSQL_HOST", "localhost");
        String port = setting("MYSQL_PORT", "3306");
        String database = setting("MYSQL_DATABASE", "vpm_clubconnect");
        String username = setting("MYSQL_USERNAME", "clubconnect_app");
        String password = setting("MYSQL_PASSWORD", null);
        String mysqlUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Kolkata";

        try (Connection h2 = DriverManager.getConnection(
                "jdbc:h2:file:./data/clubconnectdb;AUTO_SERVER=TRUE", "sa", "");
             Connection mysql = DriverManager.getConnection(mysqlUrl, username, password)) {
            mysql.setAutoCommit(false);
            try (Statement statement = mysql.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : TABLES) {
                    statement.executeUpdate("DELETE FROM `" + table + "`");
                }
            }

            for (String table : TABLES) {
                System.out.printf("Copied %d row(s) into %s.%n", copyTable(h2, mysql, table), table);
            }

            try (Statement statement = mysql.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            mysql.commit();
            System.out.println("H2-to-MySQL migration completed successfully.");
        }
    }

    private static int copyTable(Connection source, Connection target, String table) throws SQLException {
        try (Statement sourceStatement = source.createStatement();
             ResultSet rows = sourceStatement.executeQuery("SELECT * FROM \"" + table.toUpperCase() + "\"")) {
            ResultSetMetaData metadata = rows.getMetaData();
            int columnCount = metadata.getColumnCount();
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    columns.append(", ");
                    placeholders.append(", ");
                }
                columns.append('`').append(metadata.getColumnLabel(i)).append('`');
                placeholders.append('?');
            }
            String insert = "INSERT INTO `" + table + "` (" + columns + ") VALUES (" + placeholders + ")";
            int copied = 0;
            try (PreparedStatement destinationStatement = target.prepareStatement(insert)) {
                while (rows.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        destinationStatement.setObject(i, rows.getObject(i));
                    }
                    destinationStatement.addBatch();
                    copied++;
                }
                if (copied > 0) {
                    destinationStatement.executeBatch();
                }
            }
            return copied;
        }
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalStateException(name + " must be configured before migration.");
        }
        return value;
    }
}
