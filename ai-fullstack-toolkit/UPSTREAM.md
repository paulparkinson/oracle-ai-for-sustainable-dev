# Upstream baseline and provenance

`upstream/oracle-db-mcp-java-toolkit` is a clean, pinned source snapshot of Oracle's [Oracle Database MCP Java Toolkit](https://github.com/oracle/mcp/tree/main/src/oracle-db-mcp-java-toolkit), from upstream commit `5bb406b5b70e109a749cd16cc026422134a117b2`.

It is retained as the functional MCP reference and under its included UPL license. Generated `target/` content and local metadata are deliberately excluded. This snapshot is not built as part of this reactor and has no local modifications. The new three-artifact toolkit layers a portable tool model and A2A/A2UI/MCP App projections beside it; a future extraction can incrementally route the upstream database tools through the shared registry without losing the upstream implementation history.

The `tools:` YAML described in this project is application configuration for the shared registry, not a standardized MCP configuration file. MCP standardizes protocol messages; server configuration remains implementation-specific.
