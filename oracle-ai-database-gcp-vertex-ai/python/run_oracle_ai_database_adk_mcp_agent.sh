#!/bin/bash
set -e

# Load environment variables if present
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Activate virtual environment if it exists
if [ -d ".venv" ]; then
    source .venv/bin/activate
elif [ -d "../.venv" ]; then
    source ../.venv/bin/activate
elif [ -d "venv" ]; then
    source venv/bin/activate
elif [ -d "../venv" ]; then
    source ../venv/bin/activate
fi

echo "Starting ADK Agent with MCP Toolbox..."
python oracle_ai_database_adk_mcp_agent.py "$@"
