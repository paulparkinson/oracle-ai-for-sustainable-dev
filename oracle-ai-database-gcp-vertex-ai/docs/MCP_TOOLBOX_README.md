# Oracle AI Database Agent with MCP Toolbox

This implementation combines Google's Agent Development Kit (ADK) with MCP Toolbox for Databases to create a production-ready AI agent that can query Oracle AI Database.

## Architecture

```
User Input → ADK Agent → MCP Toolbox → Oracle AI Database
                ↓            ↓              ↓
           Gemini 2.0   Connection     RAG_TAB
                        Pooling       (Vector Search)
```

## Key Features

- **ADK Agent**: Multi-step reasoning with conversation context
- **MCP Toolbox**: Production-ready database connection management
- **Oracle Vector Search**: Semantic search over RAG knowledge base
- **Tool Diversity**: RAG search + SQL execution + schema inspection

## Advantages over Oracle SQLcl MCP

| Feature | MCP Toolbox | Oracle SQLcl MCP |
|---------|-------------|------------------|
| Connection Pooling | ✅ Built-in | ❌ Single connection |
| Observability | ✅ OpenTelemetry | ❌ Limited |
| Multi-Database | ✅ 30+ databases | ❌ Oracle only |
| Authentication | ✅ Multiple methods | ⚠️  Limited |
| Stability | ✅ Production-ready | ⚠️  Experimental |

## Setup

### 1. Install MCP Toolbox

The run script will auto-download the toolbox binary, or install manually:

```bash
# Linux AMD64
export VERSION=0.24.0
curl -L -o toolbox https://storage.googleapis.com/genai-toolbox/v$VERSION/linux/amd64/toolbox
chmod +x toolbox

# macOS Apple Silicon
curl -L -o toolbox https://storage.googleapis.com/genai-toolbox/v$VERSION/darwin/arm64/toolbox
chmod +x toolbox
```

### 2. Install Python Dependencies

```bash
pip install google-adk toolbox-core oracledb python-dotenv
```

Or use the full requirements:

```bash
pip install -r requirements.txt
```

### 3. Configure Environment

Ensure your `.env` file has Oracle database credentials:

```bash
# Oracle Database
DB_USERNAME=ADMIN
DB_PASSWORD=your_password
DB_DSN=aiholodb_high  # TNS alias from tnsnames.ora
DB_WALLET_PASSWORD=your_wallet_password
DB_WALLET_DIR=/path/to/Wallet_aiholodb

# Google Cloud
GCP_PROJECT_ID=your-project
GCP_REGION=us-central1
```

### 4. Configure Tools

The `tools.yaml` file defines the Oracle database source and available tools:

```yaml
sources:
  oracle-ai-db:
    kind: oracle
    tnsAlias: ${DB_DSN}
    tnsAdmin: ${DB_WALLET_DIR}
    user: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    useOCI: true  # Required for Oracle Wallet

tools:
  search-rag-documents:
    kind: oracle-sql
    source: oracle-ai-db
    description: Search the RAG knowledge base using semantic similarity
    # ... parameters and SQL statement

toolsets:
  oracle-rag-toolset:
    - search-rag-documents
    - get-rag-stats
    - execute-sql
    # ... other tools
```

## Usage

### Run the Agent

```bash
./run_oracle_ai_database_adk_mcp_agent.sh
```

This script:
1. Checks/downloads MCP Toolbox binary
2. Installs Python dependencies if needed
3. Starts the Toolbox server with `tools.yaml`
4. Runs the ADK agent
5. Handles cleanup on exit

### Example Interaction

```
🤖 Oracle AI Database Agent - MCP Toolbox Edition
======================================================================

💬 You: What is JSON Relational Duality?

🤔 Agent: Let me search the knowledge base for information about JSON Relational Duality...

[Uses search-rag-documents tool]

JSON Relational Duality is a feature in Oracle Database 26ai that provides 
a unified view of data as both relational tables and JSON documents...

💬 You: Show me the RAG table schema

🤔 Agent: I'll get the schema information for the RAG_TAB table.

[Uses get-table-schema tool]

The RAG_TAB table has the following columns:
- ID (NUMBER): Primary key
- TEXT (VARCHAR2): Document content
- LINK (VARCHAR2): Source URL
- EMBEDDING (VECTOR): 768-dimensional vector embedding
```

## Available Tools

### 1. search-rag-documents
Search the vector knowledge base using semantic similarity.

**Parameters:**
- `query_text` (string): Search query
- `top_k` (integer): Number of results (default: 5)

### 2. get-rag-stats
Get statistics about the knowledge base (total chunks, documents).

### 3. execute-sql
Execute custom SQL queries for advanced analysis.

**Parameters:**
- `sql_query` (string): SQL statement to execute

### 4. get-table-schema
Get column information for a specific table.

**Parameters:**
- `table_name` (string): Table name to inspect

### 5. list-tables
List all tables in the current schema.

## Files

- `oracle_ai_database_adk_mcp_agent.py` - ADK agent implementation
- `run_oracle_ai_database_adk_mcp_agent.sh` - Run script
- `tools.yaml` - MCP Toolbox configuration
- `.env` - Environment variables (credentials)

## Troubleshooting

### Toolbox server won't start

Check the logs:
```bash
tail -f toolbox.log
```

Common issues:
- Invalid credentials in `.env`
- Oracle wallet not found at `DB_WALLET_DIR`
- Port 5000 already in use

### Connection errors

Verify Oracle Instant Client is installed (required for `useOCI: true`):
```bash
# Ubuntu/Debian
sudo apt-get install libaio1

# Download Oracle Instant Client
# https://www.oracle.com/database/technologies/instant-client/downloads.html
```

### Agent not finding tools

Ensure the toolset name matches in both `tools.yaml` and the Python code:
```python
tools = await self.toolbox_client.load_toolset("oracle-rag-toolset")
```

## References

- [MCP Toolbox Documentation](https://googleapis.github.io/genai-toolbox/)
- [Oracle Source Configuration](https://googleapis.github.io/genai-toolbox/resources/sources/oracle/)
- [Google ADK Documentation](https://google.github.io/adk-docs/)
- [Toolbox Python SDK](https://github.com/googleapis/mcp-toolbox-sdk-python)

## Comparison with Other Implementations

### vs oracle_ai_database_adk_agent.py
- **This (MCP)**: Uses MCP Toolbox for database operations
- **ADK Agent**: Uses custom BaseTool with direct OracleVS

**When to use MCP**: Production deployments, multiple databases, need observability

**When to use custom BaseTool**: Simple RAG-only, prototyping, learning ADK

### vs oracle_ai_database_genai_mcp.py (archived)
- **This (MCP Toolbox)**: Google's official MCP implementation
- **Archived (SQLcl)**: Oracle's experimental SQLcl MCP server

**MCP Toolbox advantages**: Stable, documented, multi-database support, better error handling
