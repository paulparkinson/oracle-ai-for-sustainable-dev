# Flynn's Theme Park Java Memory Demo

This browser app has two complementary lanes:

- a live `ojdbc-agent-memory` proof-of-concept library flow using Ollama
  extraction, Oracle AI Database `ALLMINILM` embeddings, exact identity scope,
  semantic search, and context-card assembly;
- a repeatable presenter flow with fixed Java and SQL actions for correction, TTL, experience traces,
  human-approved procedural learning, and cross-guest isolation.

## Prerequisites

- Java 21
- Maven
- a working Oracle Database connection in `financial/setup/.env` or
  `memory/.env`
- Ollama running locally with `llama3.2:latest`
- `ALLMINILM` loaded in the connected Oracle Database schema
- a local clone of the `ojdbc-agent-memory` proof-of-concept library

One-time model setup:

```bash
brew install ollama
brew services start ollama
ollama pull llama3.2

cd memory/test-utils
./load-allminilm.sh
```

## Test and run

```bash
cd memory/java-agent
export OAM_LIBRARY_DIR=/path/to/ojdbc-agent-memory
./test.sh
./run.sh
```

Open <http://127.0.0.1:8091>. In another terminal, run the complete API proof:

```bash
cd memory/java-agent
./smoke-test.sh
```

The live library controls are intended to be used in order:

1. **Reset library lane**
2. **Retain Ava's conversation**
3. **Recall and build context**

Then use the seven numbered presenter controls to show the governed memory
lifecycle and continual-learning concepts.

At the bottom of the page, the database inspector shows current contents from
the two `OAMJ_CONCIERGE_*` library tables and five `AIM_DEMO_*` lifecycle
tables. It refreshes automatically after every action. **Refresh table
contents** performs the same check manually without reloading the page. Added,
changed, and removed rows remain highlighted until the next refresh.
