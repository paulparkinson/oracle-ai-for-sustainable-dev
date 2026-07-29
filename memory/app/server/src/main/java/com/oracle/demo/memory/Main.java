package com.oracle.demo.memory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import oracle.ucp.jdbc.PoolDataSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main {
    private final MemoryRepository repository;
    private final Path webRoot;

    private Main(Path webRoot, MemoryRepository repository) {
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.repository = repository;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("MEMORY_PORT", "8091"));
        Path webRoot = Path.of(System.getenv().getOrDefault("MEMORY_WEB_ROOT", "../web"));
        PoolDataSource dataSource =
                UcpDataSourceConfiguration.fromEnvironment(System.getenv());
        new DatabaseSetup(dataSource).initialize();
        new Main(webRoot, new MemoryRepository(dataSource)).start(port);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", port),
                0);
        server.createContext("/api/health", this::health);
        server.createContext("/api/state", this::state);
        server.createContext("/api/actions", this::action);
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("Memories Are the Magic listening on http://127.0.0.1:" + port);
    }

    private void health(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, Map.of("error", "GET required"));
            return;
        }
        run(exchange, repository::health);
    }

    private void state(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, Map.of("error", "GET required"));
            return;
        }
        run(exchange, repository::state);
    }

    private void action(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, Map.of("error", "POST required"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String name = path.length() > "/api/actions/".length()
                ? path.substring("/api/actions/".length())
                : "";
        try {
            Map<String, Object> body = Json.object(exchange.getRequestBody());
            Map<String, Object> result = switch (name) {
                case "reset" -> repository.reset();
                case "retain" -> repository.retain();
                case "recall" -> repository.recall(
                        string(body.get("guestId")),
                        string(body.get("query")));
                case "correct" -> repository.correct();
                case "expire" -> repository.expireOperationalMemory();
                case "dream" -> repository.dream();
                case "approve" -> repository.approveSkill(string(body.get("approver")));
                case "next-day" -> repository.nextDay();
                default -> throw new IllegalArgumentException("Unknown demo action: " + name);
            };
            respond(exchange, 200, result);
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            respond(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private void staticFile(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.equals("/") ? "index.html" : requestPath.substring(1);
        if (!filename.matches("[A-Za-z0-9._/-]+") || filename.contains("..")) {
            respond(exchange, 400, Map.of("error", "Invalid path"));
            return;
        }
        Path file = webRoot.resolve(filename).normalize();
        if (!file.startsWith(webRoot)
                || !Files.isRegularFile(file)) {
            respond(exchange, 404, Map.of("error", "Not found"));
            return;
        }
        String type = filename.endsWith(".js")
                ? "text/javascript"
                : filename.endsWith(".css")
                        ? "text/css"
                        : filename.endsWith(".svg")
                                ? "image/svg+xml"
                                : "text/html";
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void run(HttpExchange exchange, Work work) throws IOException {
        try {
            respond(exchange, 200, work.execute());
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            respond(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            Map<String, ?> value) throws IOException {
        byte[] bytes = Json.value(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    @FunctionalInterface
    private interface Work {
        Map<String, Object> execute();
    }
}
