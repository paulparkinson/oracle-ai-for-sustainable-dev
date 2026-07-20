package com.oracle.demo.interactiveai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main {
    private final RiskRepository repository = new DemoRiskRepository();
    private final ApprovalService approvals = new ApprovalService();
    private final AguiRunService runs = new AguiRunService(repository, approvals);
    private final String actor = System.getenv().getOrDefault("REQUESTED_BY", "demo.user@example.com");
    private final Path webRoot;

    private Main(Path webRoot) {
        this.webRoot = webRoot;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("AGENT_PORT", "8080"));
        Path webRoot = Path.of(System.getProperty("web.root", "../web-client"));
        new Main(webRoot).start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/runs", this::run);
        server.createContext("/api/approve", this::approve);
        server.createContext("/api/reject", this::reject);
        server.createContext("/api/health", exchange -> respond(exchange, 200, "application/json", "{\"status\":\"UP\",\"mode\":\"demo\"}"));
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("Account-risk demo listening on http://127.0.0.1:" + port);
    }

    private void run(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { respond(exchange, 405, "text/plain", "POST required"); return; }
        try {
            Map<String, String> form = form(exchange);
            double minimumRisk = Double.parseDouble(form.getOrDefault("minimumRisk", "75"));
            int maximumRows = Integer.parseInt(form.getOrDefault("maximumRows", "10"));
            InputValidation.minimumRisk(minimumRisk);
            InputValidation.maximumRows(maximumRows);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                runs.stream(output, minimumRisk, maximumRows, actor);
            }
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, "application/json", Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void approve(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { respond(exchange, 405, "text/plain", "POST required"); return; }
        try {
            Map<String, String> form = form(exchange);
            long customerId = Long.parseLong(required(form, "customerId"));
            String approvalId = required(form, "approvalId");
            String actionType = required(form, "actionType");
            String notes = required(form, "actionNotes");
            InputValidation.followUp(customerId, actionType, notes, actor);
            approvals.consume(approvalId, customerId, actor);
            ActionResult result = repository.createFollowUp(customerId, actionType, notes, actor);
            System.out.printf("audit tool=create-customer-follow-up actor=%s customerId=%d result=APPROVED actionId=%d%n", actor, customerId, result.actionId());
            respond(exchange, 200, "application/json", Json.value(Map.of(
                    "actionId", result.actionId(), "customerId", result.customerId(),
                    "actionType", result.actionType(), "status", result.status())));
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, "application/json", Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void reject(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { respond(exchange, 405, "text/plain", "POST required"); return; }
        try {
            Map<String, String> form = form(exchange);
            approvals.reject(required(form, "approvalId"), actor);
            System.out.printf("audit tool=create-customer-follow-up actor=%s result=REJECTED%n", actor);
            respond(exchange, 200, "application/json", "{\"status\":\"REJECTED\"}");
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, "application/json", Json.value(Map.of("error", exception.getMessage())));
        }
    }

    private void staticFile(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.equals("/") ? "index.html" : requestPath.substring(1);
        if (!filename.matches("[A-Za-z0-9._/-]+") || filename.contains("..")) { respond(exchange, 400, "text/plain", "Invalid path"); return; }
        Path file = webRoot.resolve(filename).normalize();
        if (!file.startsWith(webRoot.normalize()) || !Files.isRegularFile(file)) { respond(exchange, 404, "text/plain", "Not found"); return; }
        String type = filename.endsWith(".js") ? "text/javascript" : filename.endsWith(".css") ? "text/css" : "text/html";
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> result = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isBlank()) continue;
            String[] pieces = pair.split("=", 2);
            result.put(URLDecoder.decode(pieces[0], StandardCharsets.UTF_8), URLDecoder.decode(pieces.length > 1 ? pieces[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String required(Map<String, String> form, String name) {
        String value = form.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
