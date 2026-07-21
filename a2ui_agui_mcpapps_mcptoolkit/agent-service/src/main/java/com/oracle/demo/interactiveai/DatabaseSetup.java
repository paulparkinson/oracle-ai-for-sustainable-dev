package com.oracle.demo.interactiveai;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Guarded local setup runner for environments where SQLcl is unavailable. */
public final class DatabaseSetup {
    private static final Set<String> CORE_OBJECTS = Set.of(
            "CUSTOMER_ACCOUNTS",
            "CUSTOMER_RISK_EVENTS",
            "CUSTOMER_ACTIONS",
            "CREATE_CUSTOMER_FOLLOW_UP",
            "CREATE_CUSTOMER_FOLLOW_UP_MCP",
            "CUSTOMER_ACTION_MCP_SEQ",
            "ACCOUNT_RISK_SUMMARY_V",
            "ACCOUNT_RISK_EVENT_V");
    private static final Set<String> LEGACY_CORE_OBJECTS = Set.of(
            "CUSTOMER_ACCOUNTS",
            "CUSTOMER_RISK_EVENTS",
            "CUSTOMER_ACTIONS",
            "CREATE_CUSTOMER_FOLLOW_UP",
            "ACCOUNT_RISK_SUMMARY_V",
            "ACCOUNT_RISK_EVENT_V");
    private static final Set<String> MCP_SEQUENCE_RECOVERY_STATE = Set.of(
            "CUSTOMER_ACCOUNTS",
            "CUSTOMER_RISK_EVENTS",
            "CUSTOMER_ACTIONS",
            "CREATE_CUSTOMER_FOLLOW_UP",
            "CUSTOMER_ACTION_MCP_SEQ",
            "ACCOUNT_RISK_SUMMARY_V",
            "ACCOUNT_RISK_EVENT_V");
    private static final Set<String> EMPTY_TABLE_RECOVERY_STATE = Set.of(
            "CUSTOMER_ACCOUNTS", "CUSTOMER_RISK_EVENTS", "CUSTOMER_ACTIONS");

    private DatabaseSetup() {
    }

    public static void main(String[] args) throws Exception {
        Path databaseRoot = Path.of(System.getProperty("database.root", "../database")).toAbsolutePath().normalize();
        PoolDataSource dataSource = UcpDataSourceConfiguration.fromEnvironment(System.getenv());
        try (Connection connection = dataSource.getConnection()) {
            Set<String> existing = existingCoreObjects(connection);
            if (existing.equals(CORE_OBJECTS)) {
                System.out.println("Account-risk database objects already exist; no setup changes were made.");
                return;
            }
            boolean upgradingForMcp = existing.equals(LEGACY_CORE_OBJECTS);
            boolean recoveringMcpProcedure = existing.equals(MCP_SEQUENCE_RECOVERY_STATE);
            boolean recoveringEmptyTables = existing.equals(EMPTY_TABLE_RECOVERY_STATE) && tablesAreEmpty(connection);
            if (!existing.isEmpty() && !recoveringEmptyTables && !upgradingForMcp && !recoveringMcpProcedure) {
                throw new IllegalStateException("Refusing setup because a partial account-risk schema exists: " + existing);
            }

            if (recoveringMcpProcedure) {
                System.out.println("Resuming MCP setup after the sequence was created; existing objects are preserved.");
            } else if (upgradingForMcp) {
                System.out.println("Adding the purpose-built MCP sequence and stored procedure to the existing schema.");
            } else if (recoveringEmptyTables) {
                System.out.println("Resuming setup from the verified empty-table state; no objects will be dropped.");
            } else {
                System.out.println("No account-risk core objects exist; installing the schema and seed data.");
                executeSqlScript(connection, databaseRoot.resolve("01-schema.sql"));
            }
            if (!upgradingForMcp && !recoveringMcpProcedure) {
                executeSqlScript(connection, databaseRoot.resolve("02-seed-data.sql"));
                executePlsqlScript(connection, databaseRoot.resolve("03-procedures.sql"));
                executeSqlScript(connection, databaseRoot.resolve("04-views.sql"));
            }
            if (!recoveringMcpProcedure) executeSqlScript(connection, databaseRoot.resolve("05-mcp-sequence.sql"));
            executePlsqlScript(connection, databaseRoot.resolve("06-mcp-procedure.sql"));

            Set<String> installed = existingCoreObjects(connection);
            if (!installed.equals(CORE_OBJECTS)) {
                throw new IllegalStateException("Database setup did not create every expected object: " + installed);
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM customer_accounts")) {
                result.next();
                System.out.println("Account-risk database setup complete; customer rows=" + result.getInt(1));
            }
        } finally {
            destroyPool(dataSource);
        }
    }

    private static Set<String> existingCoreObjects(Connection connection) throws SQLException {
        String sql = """
                SELECT object_name
                  FROM user_objects
                 WHERE object_name IN (
                       'CUSTOMER_ACCOUNTS', 'CUSTOMER_RISK_EVENTS', 'CUSTOMER_ACTIONS',
                       'CREATE_CUSTOMER_FOLLOW_UP', 'CREATE_CUSTOMER_FOLLOW_UP_MCP',
                       'CUSTOMER_ACTION_MCP_SEQ', 'ACCOUNT_RISK_SUMMARY_V', 'ACCOUNT_RISK_EVENT_V')
                   AND object_type IN ('TABLE', 'PROCEDURE', 'VIEW', 'SEQUENCE')
                """;
        Set<String> objects = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) objects.add(rows.getString(1).toUpperCase(Locale.ROOT));
        }
        return Set.copyOf(objects);
    }

    private static boolean tablesAreEmpty(Connection connection) throws SQLException {
        for (String table : List.of("customer_accounts", "customer_risk_events", "customer_actions")) {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                if (result.getInt(1) != 0) return false;
            }
        }
        return true;
    }

    private static void executeSqlScript(Connection connection, Path script) throws IOException, SQLException {
        for (String sql : splitStatements(readScript(script))) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private static void executePlsqlScript(Connection connection, Path script) throws IOException, SQLException {
        String sql = readScript(script).replaceFirst("(?m)^/\\s*$", "").trim();
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String readScript(Path script) throws IOException {
        StringBuilder sql = new StringBuilder();
        for (String line : Files.readAllLines(script)) {
            String trimmed = line.stripLeading().toUpperCase(Locale.ROOT);
            if (trimmed.startsWith("WHENEVER SQLERROR") || trimmed.startsWith("PROMPT ")) continue;
            sql.append(line).append('\n');
        }
        return sql.toString().trim();
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            if (character == '\'' && quoted && index + 1 < script.length() && script.charAt(index + 1) == '\'') {
                current.append(character).append(script.charAt(++index));
                continue;
            }
            if (character == '\'') quoted = !quoted;
            if (character == ';' && !quoted) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) statements.add(statement);
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        String statement = current.toString().trim();
        if (!statement.isEmpty()) statements.add(statement);
        return List.copyOf(statements);
    }

    private static void destroyPool(PoolDataSource dataSource) {
        try {
            UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
                    .destroyConnectionPool(dataSource.getConnectionPoolName());
        } catch (UniversalConnectionPoolException exception) {
            throw new IllegalStateException("Unable to close Oracle UCP after database setup", exception);
        }
    }
}
