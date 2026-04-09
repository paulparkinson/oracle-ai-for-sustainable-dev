# Oracle AI Database GCP Vertex AI Demo

This repo is set up for multiple A2A-style agents. A common pattern in this repo is:

1. A caller sends a user message to an agent over A2A.
2. The agent runtime interprets that message, either with a Gemini model or with deterministic application logic.
3. The agent decides whether to call one of its local tools.
4. The tool runs locally and returns structured data.
5. The agent returns the final A2A response, often as a text message, an image artifact, or both.

## Shared Mental Model

There are three separate pieces involved in every agent:

- Client or orchestrator: Sends the user's message to the agent. In local development this is usually `test.sh` or `test.py`. In production this could be Gemini Enterprise or another A2A caller.
- Agent runtime: The service that receives the message and runs either deterministic routing or a model-driven reasoning loop.
- Tool code: Local functions such as `generate_warehouse_map(...)` that do the specialized work.

That means the local test harness is **not** acting as Gemini itself. It is acting as the caller. When an agent is model-driven, the Gemini or Vertex-backed model call happens inside the running agent service.

## Why Credentials Are Needed

The local tool functions do not need a Gemini API key by themselves. A running model-driven agent does need Google model credentials, because the agent is still LLM-driven:

- The model reads the incoming user message.
- The model decides whether a tool should be called.
- The model incorporates the tool result into the final response.

Without Gemini API or Vertex AI credentials, a model-driven flow fails before tool selection, even if the tool logic is completely local.

If you build a purely deterministic agent endpoint that directly calls local code without an LLM reasoning step, then that endpoint would not need Gemini/Vertex model credentials.

The current sample agents in this repo are now both deterministic and image-first:

- the Python spatial agent returns a PNG map artifact
- the Java graph agent returns a PNG graph artifact

So the current local demo does not require Vertex AI or Gemini API auth to answer requests. The shared Vertex settings are still useful repo infrastructure if you add a model-driven agent later.

## Shared Repo Configuration

The repo root has a shared config file at [`.env`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/.env). Agent scripts can source this so we only maintain credentials, ports, model names, and related settings in one place.

This repo now defaults to Vertex AI in that file:

- `GOOGLE_GENAI_USE_VERTEXAI=true`
- `GOOGLE_CLOUD_PROJECT`
- `GOOGLE_CLOUD_LOCATION`

Important shared values:

- `GOOGLE_API_KEY`
- `GOOGLE_GENAI_USE_VERTEXAI`
- `GOOGLE_CLOUD_PROJECT`
- `GOOGLE_CLOUD_LOCATION`
- `GOOGLE_IMPERSONATE_SERVICE_ACCOUNT`
- `PUBLIC_PROTOCOL`
- `PUBLIC_HOST`
- `BIND_HOST`
- `PORT`
- `SPATIAL_AGENT_PORT`
- `GRAPH_AGENT_PORT`
- `A2A_URL`
- `SPATIAL_AGENT_URL`
- `GRAPH_AGENT_URL`
- `MODEL_NAME`

## Credentials

You have two common options:

### Gemini API

If you use this path, you need a Gemini API key.

Create or manage a Gemini API key in Google AI Studio, then place it in:

```bash
GOOGLE_API_KEY="your-api-key"
```

Official docs:

- Gemini API keys: https://ai.google.dev/gemini-api/docs/api-key
- Gemini quickstart: https://ai.google.dev/gemini-api/docs/quickstart

### Vertex AI

If you use this path, you do not need a Gemini API key. Your local app uses
Application Default Credentials from `gcloud auth application-default login`.

Use Vertex AI project settings instead:

```bash
GOOGLE_GENAI_USE_VERTEXAI=true
GOOGLE_CLOUD_PROJECT="your-gcp-project"
GOOGLE_CLOUD_LOCATION="us-central1"
```

Authenticate ADC with the repo helper:

```bash
./auth.sh
```

`auth.sh` sources the repo [`.env`](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/.env), runs `gcloud auth application-default login`, and then runs `gcloud auth application-default set-quota-project` for `GOOGLE_CLOUD_PROJECT`.

If you want to use service account impersonation, set:

```bash
GOOGLE_IMPERSONATE_SERVICE_ACCOUNT="service-account@your-project.iam.gserviceaccount.com"
```

Official docs:

- Vertex AI auth setup: https://cloud.google.com/vertex-ai/generative-ai/docs/start/gcp-auth
- Vertex AI Gemini quickstart: https://cloud.google.com/vertex-ai/generative-ai/docs/start/quickstarts/try-gen-ai

Inference from Google's docs: the Vertex AI ADC path is the better fit for this repo if you want shared GCP auth across multiple agents.

## Agent Directories

- [oracle_spatial_agent_python](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_spatial_agent_python/README.md): Python A2A agent for warehouse map / spatial workflows. The current version is deterministic and returns a PNG artifact.
- [oracle_agent_java](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/README.md): shared Java/Spring Boot A2A runtime. The current implementation is graph-first, but the same process now serves multiple agent cards and is the natural home for future Java-side orchestration.

## Demo Flow

The current demo storyboard is:

1. Gemini Enterprise calls an Oracle AI Database agent to identify inventory risk, such as likely stockouts next quarter.
2. The same conversation drills into which warehouses, counties, or regions are driving the risk while preserving conversational context.
3. A spatial specialist renders the hotspot map so the user can see where the pressure is concentrated.
4. A graph specialist renders the supplier dependency chain so the user can see why the risk exists.
5. A Deep Research or external-intel step layers on weather, geopolitics, or other outside factors that may worsen or reduce the risk.
6. A final multi-agent action step proposes an inventory move, such as shifting units from one warehouse to another, and turns insight into an operational recommendation.

## Recommended Java Orchestration

The best use of the Java ADK pieces here is as an orchestrator around the existing specialist agents, not as a replacement for the deterministic graph renderer.

Recommended shape:

- Keep the current A2A specialists focused and deterministic. The Python spatial agent should keep owning map rendering, and the Java runtime should keep owning graph rendering and any Java-heavy business logic.
- Add a Java ADK coordinator for the final action stage. Use an `LlmAgent` for intent interpretation and recommendation synthesis, `ParallelAgent` for concurrent evidence gathering, `SequentialAgent` for the overall decision pipeline, and `FunctionTool` wrappers for policy checks and action execution.
- Treat the existing specialist agents as downstream tools or A2A calls. The coordinator should gather risk rows, map output, graph output, and external research, then write normalized fields into shared session state before making a recommendation.
- Put the actual inventory move behind a human or policy gate. The right last step for the demo is not immediate execution; it is proposal, validation, approval, and only then execution.

A good final-stage pipeline would be:

1. Intake agent: convert the live conversation into a structured inventory case with SKU, affected warehouses, service-level risk, and business impact.
2. Parallel evidence step: fan out to graph, spatial, and external-intel specialists and collect their outputs.
3. Decision agent: rank options such as transfer, expedite, substitute, or hold based on cost, coverage days, and operational risk.
4. Policy or approval tool: check business rules, thresholds, and whether a human approval is required.
5. Execution tool: create the transfer or replenishment action in the target system only after the approval result is positive.

That is the point where ADK Java becomes especially appropriate: it gives us a clean way to maintain shared state across the conversation, compose specialized agents, and add a human-in-the-loop approval stage for the "what should we do about inventory?" moment.

## Current Java ADK Implementation

The first cut of that final-stage action flow now lives in the shared Java runtime at [oracle_agent_java](/Users/pparkins/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/oracle-ai-database-gcp-vertex-ai/oracle_agent_java/README.md).

What is implemented today:

- a dedicated inventory-action A2A agent surface served by the same Spring Boot process at `/inventory-action`
- a separate action card import URL at `https://34.48.146.146/agent-card-action.json`
- a Google ADK Java coordinator that uses a `ParallelAgent` for evidence gathering and a final `LlmAgent` for recommendation synthesis
- tool-backed graph evidence from the live Oracle graph path
- seeded spatial and external evidence tools so we can demonstrate the full multi-agent arc without splitting the runtime yet
- a policy check and draft transfer tool so the final answer can explicitly say whether approval is required

What still comes next:

- swap the seeded spatial and external tools for real downstream A2A or MCP-backed calls
- persist richer shared state across multi-turn conversations
- optionally separate the action coordinator into its own runtime once the demo shape settles
