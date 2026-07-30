# Python Oracle AI Agent Memory Demo

This implementation uses the public `oracleagentmemory==26.6.0` package and Oracle AI Database.

It intentionally uses:

- `SearchStrategy.KEYWORD` with Oracle Text and `SearchIndexSyncMode.ON_COMMIT`;
- no external embedding or LLM key;
- explicit durable memory writes;
- exact user and agent scopes;
- threads and persisted messages;
- fact, preference, memory, and guideline record types;
- whole-record updates through `update_memory()`;
- per-record time-to-live;
- metadata-filtered retrieval; and
- a separately scoped, human-approved guideline.

Run:

```bash
cd memory/python-agent
./run.sh
```

Open <http://127.0.0.1:8092>. In a second terminal:

```bash
cd memory/python-agent
./smoke-test.sh
```

The runner reuses `financial/setup/.env` when `memory/.env` is absent. It never prints the database or wallet passwords.
