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
3. Choose one or both provider paths:
   - OpenAI: run `create_openai_select_ai_credential.sql`, then
     `create_paulparkdb_openai_select_ai_profile.sql` as `FINANCIAL`.
   - Google: obtain a paid Google AI Studio API key and run
     `create_google_select_ai_credential.sql`, then
     `create_paulparkdb_select_ai_profile.sql` as `FINANCIAL`.
   Both credential scripts accept their key with input echo disabled, and the
   profiles have different names so they can coexist.
   On the GCP VM, `run_paulparkdb_openai_profile_setup.sh` performs the ADMIN
   preparation and both OpenAI steps using the ignored repo-level `.env`.
   For later API-key rotation, run
   `update_openai_select_ai_credential.sql` as `FINANCIAL`; it updates the
   encrypted credential in place and does not recreate the profile.
4. Verify the selected profile with its included `showsql` query before making
   it the active managed-agent profile.
5. Fetch and run Oracle's pinned official `oracle_ai_database_agent_tool.sql`
   and `oracle_ai_database_agent.sql` installers as described in
   `../docs/ADB_PM_PROD_REDEPLOYMENT.md`.

The ADMIN preparation script grants only `CREATE PROPERTY GRAPH`, execute on the
two Select AI packages, and outbound HTTP access to the documented Google and
OpenAI endpoints. Both provider profiles are intentionally narrow and refuse to
overwrite an existing credential or profile.

On Oracle AI Database 26ai, `DBMS_CLOUD` can be a public synonym for a
patch-versioned package object. The ADMIN script discovers that synonym target
and grants the real package rather than hard-coding a patch-specific name. The
runner feeds database passwords and the provider key to SQLcl over standard
input so they do not appear in SQLcl process arguments.
