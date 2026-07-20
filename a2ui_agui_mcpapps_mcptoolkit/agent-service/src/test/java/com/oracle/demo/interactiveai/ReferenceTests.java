package com.oracle.demo.interactiveai;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReferenceTests {
    @Test
    void ucpConfigurationUsesFinancialDatabaseDefaults() throws Exception {
        var dataSource = UcpDataSourceConfiguration.fromEnvironment(Map.of(
                "TNS_ADMIN", "/tmp/Wallet_financialdb",
                "DB_PASSWORD", "test-only-placeholder"));
        assertEquals("FINANCIAL", dataSource.getUser());
        assertEquals("jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=/tmp/Wallet_financialdb", dataSource.getURL());
        assertEquals("InteractiveAiFinancialUcpPool", dataSource.getConnectionPoolName());
        assertEquals(4, dataSource.getMaxPoolSize());
    }

    @Test
    void readIsBoundedAndSorted() {
        var repository = new DemoRiskRepository();
        List<Account> accounts = repository.findAtRisk(80, 3);
        assertEquals(3, accounts.size());
        assertTrue(accounts.get(0).riskScore() >= accounts.get(1).riskScore());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DemoRiskRepository().findAtRisk(101, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DemoRiskRepository().createFollowUp(1, "DROP_TABLE", "bad", "tester@example.com"));
    }

    @Test
    void rejectedApprovalMakesNoWrite() {
        var repository = new DemoRiskRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue("tester@example.com", repository.findAtRisk(90, 2));
        approvals.reject(id, "tester@example.com");
        assertEquals(0, repository.actionCount());
    }

    @Test
    void approvedWriteIsSingleUse() {
        var repository = new DemoRiskRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue("tester@example.com", repository.findAtRisk(90, 2));
        approvals.consume(id, 1, "tester@example.com");
        repository.createFollowUp(1, "REVIEW", "Review current risk evidence", "tester@example.com");
        assertEquals(1, repository.actionCount());
        assertThrows(IllegalArgumentException.class, () -> approvals.consume(id, 1, "tester@example.com"));
    }

    @Test
    void streamUsesOfficialEventNamesAndA2uiEnvelope() throws Exception {
        var output = new ByteArrayOutputStream();
        new AguiRunService(new DemoRiskRepository(), new ApprovalService()).stream(output, 90, 2, "tester@example.com");
        String stream = output.toString(StandardCharsets.UTF_8);
        assertTrue(stream.contains("\"type\":\"RUN_STARTED\""));
        assertTrue(stream.contains("\"type\":\"TOOL_CALL_RESULT\""));
        assertTrue(stream.contains("\"name\":\"a2ui.message\""));
        assertTrue(stream.contains("\"version\":\"v0.9.1\""));
    }
}
