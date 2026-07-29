# Memories Are the Magic

This directory contains two articles, two runnable Oracle AI Database demos, and
a subtitle-led walkthrough video:

- [`blog.html`](blog.html): presenter-ready article and walkthrough based on the “Memories Are the Magic” presentation.
- [`comparisons.html`](comparisons.html): comparison of commonly evaluated AI agent-memory platforms.
- [`python-app/`](python-app/): public Oracle AI Agent Memory 26.6 SDK reference
  and browser UI.
- [`app/`](app/): Java 21, Oracle JDBC/UCP, explicit SQL mechanics, and browser UI.
- [`database/`](database/): reference schema and SQLcl inspection queries.
- [`video/`](video/): reproducible silent MP4, poster, SRT, WebVTT, and build source.

## What the demo proves

The Starlight Springs concierge demonstrates:

1. working, episodic, semantic, operational, and procedural state;
2. Retain, Recall, Reuse, and Refine;
3. guest-and-agent scope before recall;
4. transactional correction with auditable versions;
5. expiration of short-lived state;
6. experience traces as learning material;
7. human approval of an induced shared skill; and
8. reuse of that skill by a second guest with zero private-memory leakage.

The extraction and skill-induction rules are deterministic so a live
presentation is reproducible. The Oracle Database operations are real. The
Python version uses Oracle AI Agent Memory directly; the Java version exposes
the equivalent lifecycle and transaction mechanics for teaching.

## Run

The runners reuse `financial/setup/.env` if it exists. Otherwise:

```bash
cp memory/.env.example memory/.env
# Edit memory/.env. Do not commit it.
```

Run the public Python SDK reference first:

```bash
cd memory/python-app
./test.sh
./run.sh
```

Open <http://127.0.0.1:8092>. Its end-to-end API test is:

```bash
cd memory/python-app
./smoke-test.sh
```

Run the Java reference with Java 21 and Maven:

```bash
cd memory/app
./test.sh
./run.sh
```

Open <http://127.0.0.1:8091>. In a second terminal, run:

```bash
cd memory/app
./smoke-test.sh
```

The Java service creates only missing `AIM_DEMO_*` objects. The Reset button
deletes rows only from those demo tables; it does not drop schema objects. The
Python service uses SDK-managed objects with the `MAGIC_PY` store ID.

## Inspect the database

After running the browser walkthrough, execute [`database/inspect_demo.sql`](database/inspect_demo.sql) with SQLcl to see memory versions, recall audit records, traces, and skill approval state.
