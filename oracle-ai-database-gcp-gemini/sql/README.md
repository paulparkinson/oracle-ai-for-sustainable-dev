# Shared SQL Assets

This directory holds the shared Oracle Database setup, seed, and SQLcl helper scripts used across the Java, Python, and Go agent implementations.

Current contents:

- schema and seed SQL for the supply-chain graph demo
- schema and seed SQL for the inventory-risk and hotspot demo tables
- Select AI profile helper SQL
- SQLcl wrapper scripts that load the repo-level `.env` and run the shared SQL from one canonical location

Use this directory as the canonical home for database assets instead of treating them as Java-specific files.

For the `adb-pm-prod` / `paulparkdb` deployment, the controlled completion
sequence is:

1. Run `admin_prepare_paulparkdb_demo.sql` as `ADMIN`.
2. Run `setup_supply_chain_graph_schema.sql` as `FINANCIAL` to create the
   property graph over the already-installed relational tables.
3. Obtain a paid Google AI Studio API key and run
   `create_google_select_ai_credential.sql` as `FINANCIAL`; SQLcl accepts the
   key with input echo disabled.
4. Review the provider/model defaults, then run
   `create_paulparkdb_select_ai_profile.sql` as `FINANCIAL`.
5. Fetch and run Oracle's pinned official `oracle_ai_database_agent_tool.sql`
   and `oracle_ai_database_agent.sql` installers as described in
   `../docs/ADB_PM_PROD_REDEPLOYMENT.md`.

The ADMIN preparation script grants only `CREATE PROPERTY GRAPH`, execute on the
two Select AI packages, and outbound HTTP access to Google's documented
`generativelanguage.googleapis.com` endpoint. The profile script is
intentionally narrow and will not overwrite an existing profile.
