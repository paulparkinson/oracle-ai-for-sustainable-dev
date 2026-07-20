package com.oracle.demo.interactiveai;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ReferenceTests {
    public static void main(String[] args) throws Exception {
        readIsBoundedAndSorted();
        invalidInputIsRejected();
        rejectedApprovalMakesNoWrite();
        approvedWriteIsSingleUse();
        streamUsesOfficialEventNamesAndA2uiEnvelope();
        System.out.println("All reference tests passed.");
    }

    private static void readIsBoundedAndSorted() {
        var repository = new DemoRiskRepository();
        List<Account> accounts = repository.findAtRisk(80, 3);
        check(accounts.size() == 3, "maximumRows must be enforced");
        check(accounts.get(0).riskScore() >= accounts.get(1).riskScore(), "results must be sorted descending");
    }

    private static void invalidInputIsRejected() {
        expectFailure(() -> new DemoRiskRepository().findAtRisk(101, 10));
        expectFailure(() -> new DemoRiskRepository().createFollowUp(1, "DROP_TABLE", "bad", "tester@example.com"));
    }

    private static void rejectedApprovalMakesNoWrite() {
        var repository = new DemoRiskRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue("tester@example.com", repository.findAtRisk(90, 2));
        approvals.reject(id, "tester@example.com");
        check(repository.actionCount() == 0, "rejection must not write");
    }

    private static void approvedWriteIsSingleUse() {
        var repository = new DemoRiskRepository();
        var approvals = new ApprovalService();
        String id = approvals.issue("tester@example.com", repository.findAtRisk(90, 2));
        approvals.consume(id, 1, "tester@example.com");
        repository.createFollowUp(1, "REVIEW", "Review current risk evidence", "tester@example.com");
        check(repository.actionCount() == 1, "approved write must create one action");
        expectFailure(() -> approvals.consume(id, 1, "tester@example.com"));
    }

    private static void streamUsesOfficialEventNamesAndA2uiEnvelope() throws Exception {
        var output = new ByteArrayOutputStream();
        new AguiRunService(new DemoRiskRepository(), new ApprovalService()).stream(output, 90, 2, "tester@example.com");
        String stream = output.toString(StandardCharsets.UTF_8);
        check(stream.contains("\"type\":\"RUN_STARTED\""), "run event missing");
        check(stream.contains("\"type\":\"TOOL_CALL_RESULT\""), "tool result missing");
        check(stream.contains("\"name\":\"a2ui.message\""), "A2UI custom event missing");
        check(stream.contains("\"version\":\"v0.9.1\""), "A2UI version missing");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected failure");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
