package com.oracle.demo.interactiveai;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class OracleUcpRiskRepository implements RiskRepository, AutoCloseable {
    private static final String FIND_AT_RISK_SQL = """
            SELECT customer_id, customer_name, industry, account_value,
                   risk_score, risk_level, risk_summary, owner_name, follow_up_status
              FROM account_risk_summary_v
             WHERE risk_score >= ?
             ORDER BY risk_score DESC
            """;
    private static final String CREATE_FOLLOW_UP_CALL = "{ call create_customer_follow_up(?, ?, ?, ?, ?) }";
    private final PoolDataSource dataSource;

    public OracleUcpRiskRepository(PoolDataSource dataSource) {
        this.dataSource = dataSource;
        verifyConnection();
    }

    @Override
    public List<Account> findAtRisk(double minimumRisk, int maximumRows) {
        InputValidation.minimumRisk(minimumRisk);
        InputValidation.maximumRows(maximumRows);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AT_RISK_SQL)) {
            setClientInfo(connection, "find-at-risk-customers");
            statement.setDouble(1, minimumRisk);
            statement.setMaxRows(maximumRows);
            statement.setQueryTimeout(15);
            try (ResultSet rows = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (rows.next()) {
                    BigDecimal accountValue = rows.getBigDecimal("account_value");
                    accounts.add(new Account(
                            rows.getLong("customer_id"),
                            rows.getString("customer_name"),
                            rows.getString("industry"),
                            accountValue == null ? 0 : accountValue.longValue(),
                            rows.getDouble("risk_score"),
                            rows.getString("risk_level"),
                            rows.getString("risk_summary"),
                            rows.getString("owner_name"),
                            rows.getString("follow_up_status")));
                }
                return List.copyOf(accounts);
            }
        } catch (SQLException exception) {
            throw databaseFailure("find-at-risk-customers", exception);
        }
    }

    @Override
    public ActionResult createFollowUp(long customerId, String actionType, String actionNotes, String requestedBy) {
        InputValidation.followUp(customerId, actionType, actionNotes, requestedBy);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            setClientInfo(connection, "create-customer-follow-up");
            try (CallableStatement statement = connection.prepareCall(CREATE_FOLLOW_UP_CALL)) {
                statement.setLong(1, customerId);
                statement.setString(2, actionType);
                statement.setString(3, actionNotes);
                statement.setString(4, requestedBy);
                statement.registerOutParameter(5, Types.NUMERIC);
                statement.setQueryTimeout(15);
                statement.execute();
                long actionId = statement.getLong(5);
                connection.commit();
                return new ActionResult(actionId, customerId, actionType, "APPROVED");
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseFailure("create-customer-follow-up", exception);
            } finally {
                restoreAutoCommit(connection);
            }
        } catch (SQLException exception) {
            throw databaseFailure("create-customer-follow-up", exception);
        }
    }

    @Override
    public int actionCount() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM customer_actions");
             ResultSet row = statement.executeQuery()) {
            row.next();
            return row.getInt(1);
        } catch (SQLException exception) {
            throw databaseFailure("count-customer-actions", exception);
        }
    }

    public void verifyConnection() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT SYS_CONTEXT('USERENV', 'CURRENT_USER'), SYS_CONTEXT('USERENV', 'SERVICE_NAME') FROM dual");
             ResultSet row = statement.executeQuery()) {
            row.next();
            System.out.printf("Oracle UCP connected as %s to service %s using pool %s%n",
                    row.getString(1), row.getString(2), dataSource.getConnectionPoolName());
        } catch (SQLException exception) {
            throw databaseFailure("database-health-check", exception);
        }
    }

    @Override
    public void close() {
        try {
            UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
                    .destroyConnectionPool(dataSource.getConnectionPoolName());
        } catch (UniversalConnectionPoolException exception) {
            throw new IllegalStateException("Unable to close Oracle UCP", exception);
        }
    }

    private static void setClientInfo(Connection connection, String action) {
        try {
            connection.setClientInfo("OCSID.MODULE", "interactive-ai-agent-service");
            connection.setClientInfo("OCSID.ACTION", action);
        } catch (SQLException exception) {
            throw databaseFailure("set-client-info", exception);
        }
    }

    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private static void restoreAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Closing the borrowed connection returns it to UCP; UCP also resets connection state.
        }
    }

    private static IllegalStateException databaseFailure(String operation, SQLException exception) {
        return new IllegalStateException("Oracle Database operation failed: " + operation + " (" + exception.getErrorCode() + ")", exception);
    }
}
