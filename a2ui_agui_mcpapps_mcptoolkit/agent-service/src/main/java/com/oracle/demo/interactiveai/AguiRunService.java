package com.oracle.demo.interactiveai;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AguiRunService {
    private final RiskRepository repository;
    private final ApprovalService approvals;

    public AguiRunService(RiskRepository repository, ApprovalService approvals) {
        this.repository = repository;
        this.approvals = approvals;
    }

    public void stream(OutputStream output, double minimumRisk, int maximumRows, String actor) throws IOException {
        InputValidation.minimumRisk(minimumRisk);
        InputValidation.maximumRows(maximumRows);
        String threadId = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();
        String toolCallId = UUID.randomUUID().toString();

        send(output, Map.of("type", "RUN_STARTED", "threadId", threadId, "runId", runId));
        send(output, Map.of("type", "STEP_STARTED", "stepName", "retrieve-governed-risk-data"));
        send(output, Map.of("type", "TEXT_MESSAGE_START", "messageId", messageId, "role", "assistant"));
        send(output, Map.of("type", "TEXT_MESSAGE_CONTENT", "messageId", messageId, "delta", "I’m querying the governed account-risk view. "));
        send(output, Map.of("type", "TOOL_CALL_START", "toolCallId", toolCallId, "toolCallName", "find-at-risk-customers", "parentMessageId", messageId));
        send(output, Map.of("type", "TOOL_CALL_ARGS", "toolCallId", toolCallId, "delta", Json.value(Map.of("minimumRisk", minimumRisk, "maximumRows", maximumRows))));

        List<Account> accounts = repository.findAtRisk(minimumRisk, maximumRows);
        String accountJson = accounts.stream().map(Json::account).reduce("[", (left, item) -> left.equals("[") ? left + item : left + "," + item) + "]";
        send(output, Map.of("type", "TOOL_CALL_END", "toolCallId", toolCallId));
        send(output, Map.of("type", "TOOL_CALL_RESULT", "messageId", UUID.randomUUID().toString(), "toolCallId", toolCallId, "content", accountJson, "role", "tool"));
        send(output, Map.of("type", "STEP_FINISHED", "stepName", "retrieve-governed-risk-data"));

        String approvalId = approvals.issue(actor, accounts);
        send(output, Map.of("type", "STATE_SNAPSHOT", "snapshot", Map.of(
                "status", "AWAITING_APPROVAL", "approvalId", approvalId,
                "accountCount", accounts.size(), "minimumRisk", minimumRisk)));
        a2ui(output, A2uiPayloads.createSurface());
        a2ui(output, A2uiPayloads.components());
        a2ui(output, A2uiPayloads.data(accounts, approvalId));
        send(output, Map.of("type", "TEXT_MESSAGE_CONTENT", "messageId", messageId, "delta", "Review the returned accounts and explicitly approve or cancel a follow-up."));
        send(output, Map.of("type", "TEXT_MESSAGE_END", "messageId", messageId));
        send(output, Map.of("type", "RUN_FINISHED", "threadId", threadId, "runId", runId, "result", Map.of("status", "AWAITING_APPROVAL")));
    }

    private void a2ui(OutputStream output, Map<String, Object> envelope) throws IOException {
        send(output, Map.of("type", "CUSTOM", "name", "a2ui.message", "value", envelope));
    }

    static void send(OutputStream output, Map<String, Object> event) throws IOException {
        output.write(("data: " + Json.value(event) + "\n\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
