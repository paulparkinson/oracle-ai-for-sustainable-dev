# Quick Start: Oracle ADK Agent with MCP

## What Was Created

### 1. **oracle_ai_database_adk_agent.py** (MCP-Enabled Version)
   - Integrates Oracle MCP Server for direct database queries
   - Uses Vector RAG for documentation search
   - Connects to `paulparkdb_mcp` for database operations

### 2. **oracle_ai_database_adk_agent_ragonlynomcp.py** (Backup)
   - Original version without MCP
   - RAG only

### 3. Runner Scripts
   - `run_adk_mcp_agent.sh` (Linux/Mac)
   - `run_adk_mcp_agent.ps1` (Windows)

### 4. Documentation
   - `ADK_AGENT_README.md` - Full documentation
   - `QUICK_START_ADK_MCP.md` - This file

## Prerequisites

1. **SQLcl with MCP support** installed at `/opt/sqlcl/bin/sql`
2. **Oracle wallet** configured (default: `~/wallet`)
3. **Saved connection** in SQLcl: `paulparkdb_mcp`
4. **Python 3.9+** with virtual environment

## Setup (5 minutes)

### Step 1: Verify SQLcl MCP Connection

```bash
# Test SQLcl MCP server
export TNS_ADMIN=~/wallet
/opt/sqlcl/bin/sql -mcp
```

You should see the MCP server start successfully.

### Step 2: Install Dependencies

```bash
cd oracle-ai-database-gcp-vertex-ai

# Create/activate virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
.\venv\Scripts\Activate.ps1  # Windows

# Install dependencies
pip install -r requirements-adk.txt
```

### Step 3: Configure Environment

Create `.env` file or export variables:

```bash
export ORACLE_RAG_API_URL="http://34.48.146.146:8501"
export GCP_PROJECT_ID="adb-pm-prod"
export GCP_REGION="us-central1"
export SQLCL_PATH="/opt/sqlcl/bin/sql"
export TNS_ADMIN="$HOME/wallet"
```

### Step 4: Run the Agent

**Linux/Mac:**
```bash
chmod +x run_adk_mcp_agent.sh
./run_adk_mcp_agent.sh
```

**Windows:**
```powershell
.\run_adk_mcp_agent.ps1
```

**Direct:**
```bash
python oracle_ai_database_adk_agent.py
```

## Example Usage

### Documentation Questions (RAG)
```
You: What are vector indexes in Oracle 23ai?
Agent: [Searches documentation and provides detailed answer]
```

### Database Queries (MCP)
```
You: Show me the most recently created tables
Agent: [Connects to paulparkdb_mcp and executes query]
```

### Combined Questions
```
You: Explain spatial indexes and check if any exist in the database
Agent: [Uses RAG for explanation, MCP for database inspection]
```

## How It Works

```
┌─────────────────────────────────┐
│  User asks question             │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  ADK Agent (Gemini 2.0)         │
│  Decides which tool to use      │
└────────────┬────────────────────┘
             │
       ┌─────┴──────┐
       │            │
       ▼            ▼
┌──────────┐  ┌──────────────┐
│ RAG API  │  │ MCP Server   │
│ (Vector) │  │ (SQLcl)      │
└──────────┘  └──────────────┘
       │            │
       ▼            ▼
┌──────────┐  ┌──────────────┐
│ Docs in  │  │ Oracle DB    │
│ RAG_TAB  │  │ paulparkdb   │
└──────────┘  └──────────────┘
```

## Agent Intelligence

The agent automatically chooses the right tool:

| Question Type | Tool Used | Example |
|--------------|-----------|---------|
| Conceptual | RAG | "What is a vector index?" |
| How-to | RAG | "How do I create spatial tables?" |
| Data query | MCP | "List all tables" |
| Schema check | MCP | "Show me table structure" |
| SQL execution | MCP | "Run SELECT * FROM stocks" |
| Combined | Both | "Explain JSON columns and show examples from DB" |

## MCP Tools Available

When connected to `paulparkdb_mcp`, the agent can use:

1. **list-connections** - Show saved connections
2. **connect** - Connect to database (uses paulparkdb_mcp)
3. **disconnect** - Close connection
4. **run-sql** - Execute SQL queries
5. **run-sqlcl** - Execute SQLcl commands
6. **schema-information** - Get detailed schema metadata

## Troubleshooting

### "Cannot connect to MCP server"

1. Verify SQLcl installation:
   ```bash
   /opt/sqlcl/bin/sql -version
   ```

2. Check saved connection:
   ```bash
   sql /nolog
   SQL> show connections
   ```

3. Test MCP mode:
   ```bash
   export TNS_ADMIN=~/wallet
   /opt/sqlcl/bin/sql -mcp
   ```

### "Falling back to RAG-only mode"

The agent will automatically fall back if MCP fails. You'll still have documentation search via RAG.

### Wallet Issues

```bash
# Verify wallet files exist
ls -la ~/wallet/
# Should show: tnsnames.ora, sqlnet.ora, ewallet.p12, cwallet.sso

# Check TNS_ADMIN environment variable
echo $TNS_ADMIN
```

## Comparison with Original

| Feature | New (MCP) | Original |
|---------|-----------|----------|
| Documentation | ✅ RAG | ✅ RAG |
| Direct DB Queries | ✅ MCP | ❌ No |
| SQL Execution | ✅ Yes | ❌ No |
| Schema Info | ✅ Yes | ❌ No |
| Tools | 6+ | 2 |

## Next Steps

1. Test with documentation questions
2. Try database queries
3. Explore combined questions
4. Check [ADK_AGENT_README.md](ADK_AGENT_README.md) for advanced usage

## Key Configuration

The MCP server is configured to connect using:

```python
oracle_mcp_params = StdioServerParameters(
    command="/opt/sqlcl/bin/sql",  # SQLCL_PATH env var
    args=["-mcp"],
    env={"TNS_ADMIN": "~/wallet"}  # TNS_ADMIN env var
)
```

The agent instructions specify:
- Use MCP connection: **paulparkdb_mcp**
- Always connect before running queries
- Use RAG for documentation, MCP for data

## Files Summary

```
oracle-ai-database-gcp-vertex-ai/
├── oracle_ai_database_adk_agent.py              # ⭐ New MCP version
├── oracle_ai_database_adk_agent_ragonlynomcp.py # 📦 Backup (RAG only)
├── oracle_ai_database_adk_fullagent.py          # 🔧 Alternative (Gemini)
├── run_adk_mcp_agent.sh                         # 🐧 Linux/Mac runner
├── run_adk_mcp_agent.ps1                        # 🪟 Windows runner
├── requirements-adk.txt                         # 📋 Updated dependencies
├── ADK_AGENT_README.md                          # 📖 Full documentation
├── EMBEDDINGS_COMPARISON.md                     # 📊 Embeddings guide
└── QUICK_START_ADK_MCP.md                       # ⚡ This quick start
```

## Support

For issues or questions:
1. Check [ADK_AGENT_README.md](ADK_AGENT_README.md)
2. Review [Google ADK docs](https://google.github.io/adk-docs/)
3. Check [MCP documentation](https://modelcontextprotocol.io/)
4. Review [ADK MCP examples](https://github.com/GoogleCloudPlatform/generative-ai/tree/main/gemini/mcp/adk_mcp_app)
