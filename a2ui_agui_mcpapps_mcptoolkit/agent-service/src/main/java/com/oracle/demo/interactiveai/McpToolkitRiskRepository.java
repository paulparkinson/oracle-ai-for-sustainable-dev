package com.oracle.demo.interactiveai;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Calls purpose-built Oracle Database MCP Java Toolkit tools over MCP stdio. */
public final class McpToolkitRiskRepository implements RiskRepository, AutoCloseable {
    private static final Set<String> REQUIRED_TOOLS = Set.of(
            "find-at-risk-customers",
            "get-customer-risk-details",
            "reserve-customer-action-id",
            "create-customer-follow-up",
            "count-customer-actions");

    private final McpStdioClient client;

    private McpToolkitRiskRepository(McpStdioClient client) {
        this.client = client;
        verifyToolkit();
    }

    public static McpToolkitRiskRepository fromEnvironment(Map<String, String> environment) {
        Path toolkitJar = requiredPath(environment, "ORACLE_MCP_TOOLKIT_JAR");
        Path configFile = optionalPath(environment, "ORACLE_MCP_CONFIG_FILE",
                Path.of("../oracle-db-mcp-toolkit/config/tools.yaml"));
        String databaseUrl = required(environment, "DB_URL");
        String databaseUser = firstNonBlank(environment.get("DB_USERNAME"), environment.get("DB_USER"));
        String databasePassword = required(environment, "DB_PASSWORD");
        if (databaseUser == null) throw new IllegalArgumentException("DB_USERNAME or DB_USER is required");

        String javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        McpStdioClient client = new McpStdioClient(
                List.of(javaCommand,
                        "-DconfigFile=" + configFile,
                        "-Dtools=account-risk",
                        "-jar", toolkitJar.toString()),
                Map.of(
                        "DB_URL", databaseUrl,
                        "DB_USERNAME", databaseUser,
                        "DB_PASSWORD", databasePassword));
        client.initialize();
        return new McpToolkitRiskRepository(client);
    }

    @Override
    public List<Account> findAtRisk(double minimumRisk, int maximumRows) {
        InputValidation.minimumRisk(minimumRisk);
        InputValidation.maximumRows(maximumRows);
        return rows(client.callTool("find-at-risk-customers", Map.of(
                "minimumRisk", minimumRisk,
                "maximumRows", maximumRows))).stream()
                .map(row -> new Account(
                        longValue(row, "CUSTOMER_ID"),
                        stringValue(row, "CUSTOMER_NAME"),
                        stringValue(row, "INDUSTRY"),
                        longValue(row, "ACCOUNT_VALUE"),
                        doubleValue(row, "RISK_SCORE"),
                        stringValue(row, "RISK_LEVEL"),
                        stringValue(row, "RISK_SUMMARY"),
                        stringValue(row, "OWNER_NAME"),
                        stringValue(row, "FOLLOW_UP_STATUS")))
                .toList();
    }

    @Override
    public ActionResult createFollowUp(long customerId, String actionType, String actionNotes, String requestedBy) {
        InputValidation.followUp(customerId, actionType, actionNotes, requestedBy);
        long actionId = longValue(firstRow(client.callTool("reserve-customer-action-id", Map.of())), "ACTION_ID");
        client.callTool("create-customer-follow-up", Map.of(
                "actionId", actionId,
                "customerId", customerId,
                "actionType", actionType,
                "actionNotes", actionNotes,
                "requestedBy", requestedBy));
        return new ActionResult(actionId, customerId, actionType, "APPROVED");
    }

    @Override
    public int actionCount() {
        return Math.toIntExact(longValue(firstRow(client.callTool("count-customer-actions", Map.of())), "ACTION_COUNT"));
    }

    @Override
    public void close() {
        client.close();
    }

    private void verifyToolkit() {
        if (!"oracle-db-mcp-toolkit".equals(client.serverName())) {
            throw new IllegalStateException("Connected MCP server is not Oracle Database MCP Java Toolkit");
        }
        Set<String> available = client.listTools();
        if (!available.equals(REQUIRED_TOOLS)) {
            throw new IllegalStateException("Oracle Database MCP Toolkit tool allowlist mismatch; expected "
                    + REQUIRED_TOOLS.stream().sorted().toList() + " but received " + available.stream().sorted().toList());
        }
        System.out.printf("MCP initialized with %s %s; enabled tools=%s%n",
                client.serverName(), client.serverVersion(), available.stream().sorted().toList());
    }

    private static List<JsonNode> rows(McpStdioClient.ToolResult result) {
        JsonNode rows = result.structuredContent().path("rows");
        if (!rows.isArray()) throw new IllegalStateException("Oracle Database MCP tool did not return structured rows");
        return StreamSupport.stream(rows.spliterator(), false).toList();
    }

    private static JsonNode firstRow(McpStdioClient.ToolResult result) {
        List<JsonNode> rows = rows(result);
        if (rows.size() != 1) throw new IllegalStateException("Oracle Database MCP tool must return exactly one row");
        return rows.getFirst();
    }

    private static JsonNode value(JsonNode row, String name) {
        JsonNode value = row.get(name);
        if (value == null || value.isNull()) value = row.get(name.toLowerCase());
        if (value == null || value.isNull()) throw new IllegalStateException("Oracle Database MCP row is missing " + name);
        return value;
    }

    private static long longValue(JsonNode row, String name) {
        JsonNode value = value(row, name);
        return value.isNumber() ? value.decimalValue().longValueExact() : Long.parseLong(value.asText());
    }

    private static double doubleValue(JsonNode row, String name) {
        JsonNode value = value(row, name);
        return value.isNumber() ? value.doubleValue() : Double.parseDouble(value.asText());
    }

    private static String stringValue(JsonNode row, String name) {
        return value(row, name).asText();
    }

    private static Path requiredPath(Map<String, String> environment, String name) {
        return checkedPath(required(environment, name), name);
    }

    private static Path optionalPath(Map<String, String> environment, String name, Path fallback) {
        String value = environment.get(name);
        return checkedPath(value == null || value.isBlank() ? fallback.toString() : value, name);
    }

    private static Path checkedPath(String value, String name) {
        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException(name + " does not identify a file: " + path);
        return path;
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }
}
