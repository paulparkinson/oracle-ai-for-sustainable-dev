# Inventory transfer example

This compact Spring Boot application uses `ai-fullstack-toolkit-starter` to register one inventory-transfer operation, then exposes its generated A2A card and A2UI example.

Run from the `ai-fullstack-toolkit` directory:

```bash
mvn -pl examples/inventory-transfer-demo -am spring-boot:run
```

Then inspect `http://localhost:8081/demo/a2a-card` and `http://localhost:8081/demo/a2ui`.

The actual transfer implementation remains your application code. The toolkit describes and projects that operation; it does not bypass application authorization or execute database actions itself.
