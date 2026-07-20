# Oracle Database MCP Java Toolkit integration

This directory contains only this demo's configuration and contracts. It deliberately does not copy Oracle's toolkit source.

1. Check out [oracle/mcp](https://github.com/oracle/mcp) as a sibling repository.
2. Build `src/oracle-db-mcp-java-toolkit` with JDK 17+ and Maven 3.9+.
3. Run it with `-Dtransport=http`, TLS/authentication settings, `-DconfigFile=<absolute-path>/config/tools.yaml`, and `-Dtools=account-risk-read`.
4. Supply database connection settings through `-Ddb.url`, `-Ddb.user`, and `-Ddb.password` or an approved centralized JDBC provider. Do not add credentials to `tools.yaml`.

Do not use `-Dtools=*` for this demo: it would enable unrestricted `write-query`, table-management, and other broad tools.

`contracts/create-customer-follow-up.json` defines the narrow write tool that the toolkit extension must expose. It calls `create_customer_follow_up` using a JDBC `CallableStatement`, registers the numeric OUT parameter, commits only after success, rolls back on any exception, and returns the action ID. Current YAML does not express OUT parameter modes, so claiming this as a YAML tool would be misleading.
