"""
Oracle AI Database Agent - ADK + SQLcl MCP (McpToolset)

Uses Google Agent Development Kit (ADK) with native McpToolset integration
to connect to Oracle Database via SQLcl's MCP server.

This version:
- Uses ADK's built-in McpToolset (no manual JSON-RPC plumbing)
- Connects to Oracle SQLcl MCP server via stdio
- Requires SQLcl with MCP support and Java runtime
- Requires google-adk>=1.25.1 (fixes schema converter bug with array properties)

Previously blocked by: https://github.com/google/adk-python issues with
_gemini_schema_util.py failing on array-type schema properties like required: ["sql"]
"""
import os
import asyncio
from contextlib import AsyncExitStack
from dotenv import load_dotenv
import vertexai
from google.adk.agents import LlmAgent
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from google.adk.artifacts.in_memory_artifact_service import InMemoryArtifactService
from google.adk.tools.mcp_tool.mcp_toolset import McpToolset, StdioServerParameters
from google.adk.tools.mcp_tool import StdioConnectionParams
from google.genai import types
from google.genai.types import GenerateContentConfig

# Load environment variables from parent directory
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), '..', '.env'))


class OracleADKSqlclMCPAgent:
    """ADK Agent using McpToolset with Oracle SQLcl MCP server"""

    def __init__(self, project_id: str, location: str,
                 sqlcl_path: str, wallet_path: str):
        self.project_id = project_id
        self.location = location
        self.sqlcl_path = sqlcl_path
        self.wallet_path = wallet_path
        self.agent = None
        self.runner = None
        self.session_service = InMemorySessionService()
        self.artifacts_service = InMemoryArtifactService()
        self.session = None
        self.exit_stack = AsyncExitStack()
        self.auth_script_path = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "..", "auth.sh")
        )

    def _is_adc_auth_error(self, error: Exception) -> bool:
        """Return True when the exception chain indicates ADC reauthentication is needed."""
        error_messages = []
        current_error = error
        while current_error is not None:
            error_messages.append(str(current_error).lower())
            current_error = current_error.__cause__ or current_error.__context__
        combined = "\n".join(error_messages)
        auth_markers = [
            "reauthentication is needed",
            "gcloud auth application-default login",
            "failed to retrieve access token",
            "invalid_grant",
            "unable to acquire impersonated credentials",
        ]
        return any(marker in combined for marker in auth_markers)

    def _print_adc_auth_help(self):
        """Print concise remediation for ADC auth failures."""
        print("\n  ⚠️  Google ADC authentication is required.")
        if os.path.exists(self.auth_script_path):
            print(f"  → Run: {self.auth_script_path}")
        print("  → Or run: gcloud auth application-default login --no-launch-browser")

    async def create_agent(self):
        """Create ADK agent with McpToolset connected to SQLcl"""
        print("  → Initializing ADK session service...")

        # Initialize Vertex AI
        vertexai.init(project=self.project_id, location=self.location)
        os.environ['GOOGLE_CLOUD_PROJECT'] = self.project_id
        os.environ['GOOGLE_CLOUD_LOCATION'] = self.location
        os.environ['GOOGLE_GENAI_USE_VERTEXAI'] = 'true'

        # Create session
        self.session = await self.session_service.create_session(
            app_name="Oracle ADK SQLcl MCP Agent",
            user_id="user_123",
            state={}
        )

        # Configure SQLcl MCP server connection
        print(f"  → Configuring SQLcl MCP: {self.sqlcl_path} -mcp")
        print(f"  → Wallet/TNS_ADMIN: {self.wallet_path}")

        mcp_connection_params = StdioConnectionParams(
            server_params=StdioServerParameters(
                command=self.sqlcl_path,
                args=["-mcp"],
                env={"TNS_ADMIN": self.wallet_path}
            ),
            protocol_version="2024-11-05"
        )

        # Create McpToolset - ADK handles MCP protocol, schema conversion, tool dispatch
        mcp_toolset = McpToolset(connection_params=mcp_connection_params)

        print("  → Starting SQLcl MCP server and discovering tools...")

        instruction = """You are an expert Oracle Database AI assistant with direct database access via MCP.

**Available Database Tools (via SQLcl MCP):**
- List available database connections with list-connections
- Connect to databases with connect
- Run SQL queries with run-sql
- Get schema information with schema-information
- Disconnect with disconnect

**AUTONOMOUS Multi-Step Workflow:**
When a user asks for database information, be proactive and autonomous:
1. First, automatically call list-connections to see available connections
2. If connections are available, automatically pick the first/most relevant one and call connect
3. Once connected, automatically call the appropriate tool (run-sql or schema-information)
4. Present the results to the user in a clear, formatted way

**DO NOT ask the user for connection names or approval - be autonomous:**
- "Show me tables" → List connections, connect to first one, run "SELECT table_name FROM user_tables"
- "What's in my database" → List connections, connect, get schema information
- "Query my data" → List connections, connect, run the appropriate SQL

**Connection Strategy:**
- When multiple connections exist, prefer connections with "prod", "main", or similar names
- If connection fails, try the next available connection
- Only ask the user for input if NO connections are available

Be helpful, technically accurate, and AUTONOMOUS - don't make users specify connection details."""

        print("  → Creating ADK LlmAgent with McpToolset...")

        self.agent = LlmAgent(
            model="gemini-2.5-flash",
            name="oracle_sqlcl_mcp_agent",
            instruction=instruction,
            tools=[mcp_toolset],
            generate_content_config=GenerateContentConfig(
                temperature=0.1,
                max_output_tokens=2048,
            )
        )

        print("  ✓ Agent created with SQLcl MCP tools")

        # Create runner
        self.runner = Runner(
            app_name="Oracle ADK SQLcl MCP Agent",
            agent=self.agent,
            artifact_service=self.artifacts_service,
            session_service=self.session_service
        )

        print("  ✓ Runner initialized")
        return self.agent

    async def query(self, user_input: str) -> str:
        """Query the agent"""
        if not self.runner or not self.session:
            raise RuntimeError("Agent not initialized. Call create_agent() first.")

        try:
            content = types.Content(
                role="user",
                parts=[types.Part(text=user_input)]
            )

            events = self.runner.run_async(
                session_id=self.session.id,
                user_id="user_123",
                new_message=content
            )

            response_parts = []
            async for event in events:
                if event.content.parts:
                    for part in event.content.parts:
                        if hasattr(part, 'function_call') and part.function_call:
                            print(f"  🔧 Tool call: {part.function_call.name}")
                        elif hasattr(part, 'function_response') and part.function_response:
                            result = part.function_response.response.get('result', '')
                            if result:
                                print(f"  ✓ Tool result received ({len(str(result))} chars)")
                        elif part.text and event.content.role == "model":
                            response_parts.append(part.text)

            return "\n".join(response_parts) if response_parts else "No response generated"

        except Exception as e:
            import traceback
            traceback.print_exc()
            return f"Error during agent reasoning: {str(e)}"

    async def cleanup(self):
        """Cleanup resources"""
        if self.runner:
            await self.runner.close()
        await self.exit_stack.aclose()
        print("  ✓ MCP server and resources cleaned up")

    async def run_cli(self):
        """Run interactive CLI"""
        print("=" * 80)
        print("Oracle Database ADK Agent (SQLcl MCP via McpToolset)")
        print("=" * 80)
        print(f"Project: {self.project_id}")
        print(f"Region: {self.location}")
        print(f"SQLcl: {self.sqlcl_path}")
        print()

        print("🔧 Initializing ADK agent with SQLcl MCP tools...")

        try:
            await self.create_agent()
            print()
            print("Type your questions about Oracle Database (or 'quit' to exit)")
            print("-" * 80)
            print()

            while True:
                try:
                    user_input = input("You: ").strip()

                    if not user_input:
                        continue

                    if user_input.lower() in ['quit', 'exit', 'q']:
                        print("\nGoodbye!")
                        break

                    response = await self.query(user_input)
                    print(f"\nAgent: {response}\n")

                except KeyboardInterrupt:
                    print("\n\nInterrupted. Goodbye!")
                    break
                except Exception as e:
                    print(f"\nError: {str(e)}\n")
                    import traceback
                    traceback.print_exc()
                    continue

            await self.cleanup()

        except Exception as e:
            if self._is_adc_auth_error(e):
                print("\n❌ Failed to initialize agent due to expired or missing ADC credentials.")
                self._print_adc_auth_help()
            else:
                print(f"\n❌ Failed to initialize agent: {str(e)}")
                import traceback
                traceback.print_exc()
            await self.cleanup()


async def main():
    """Main entry point"""
    project_id = os.getenv("GCP_PROJECT_ID", "adb-pm-prod")
    location = os.getenv("GCP_REGION", "us-central1")
    sqlcl_path = os.getenv("SQLCL_PATH", "/opt/sqlcl/bin/sql")
    wallet_path = os.getenv("TNS_ADMIN", os.path.expanduser("~/wallet"))

    print("Starting ADK + SQLcl MCP Agent...\n")

    agent = OracleADKSqlclMCPAgent(project_id, location, sqlcl_path, wallet_path)
    await agent.run_cli()


if __name__ == "__main__":
    asyncio.run(main())
