# Gemini Enterprise deployment on Google Cloud

This runbook deploys the supply-chain A2A/A2UI adapter and its private Java
service as one Cloud Run container. It targets the existing Google Cloud and
Oracle Database@Google Cloud resources discovered on 2026-07-29:

- Google Cloud project: `adb-pm-prod`
- region: `us-east4`
- Autonomous Database: `paulparkdb` (`23ai`, `OLTP`)
- wallet service: `paulparkdb_tp`
- Gemini Enterprise app: `Inventory System`
- Gemini Enterprise app ID: `inventory-system_1775523930395`
- Gemini Enterprise location: `global`

The existing `paulpark-instance` VM and its four registered A2A agents are not
modified by this deployment.

## Why Cloud Run instead of another VM

Gemini Enterprise requires a reachable HTTPS A2A endpoint, not a VM. Cloud Run
provides the managed certificate and public service URL required for agent
discovery. The selected `paulparkdb` exposes wallet-authenticated TCPS
connection strings, so this first deployment does not need a new VM or VPC
connector.

If the database is later changed to a private endpoint, add Cloud Run Direct
VPC egress to the database's VPC/subnet, or place the same container on a VM
inside that VPC. Oracle Database@Google Cloud requires the ODB network and
associated VPC to be in the same project.

## Runtime and secret boundaries

The container exposes only the Python A2A adapter on Cloud Run's `PORT`.
The Java service listens on loopback port 8081 and starts the pinned Oracle
Database MCP Java Toolkit as a stdio child:

```text
Gemini Enterprise
  -> Cloud Run HTTPS / A2A v0.3 / A2UI v0.8
  -> Python adapter :8080
  -> Java review and approval service 127.0.0.1:8081
  -> Oracle Database MCP Java Toolkit over stdio
  -> paulparkdb_tp over wallet-authenticated TCPS
```

The wallet ZIP and the `FINANCIAL` database password are separate Secret
Manager secrets. They are excluded from the Cloud Build upload by
`.gcloudignore`. The runtime service account receives Secret Manager access;
no database secret is built into the image or stored in Git.

The deployment deliberately sets `--max-instances=1` because approval handles
are currently in memory. Production should move approvals and idempotency
records to a durable store before enabling multiple instances.

## Prerequisites

1. Authenticate `gcloud` as `paul.parkinson@oracle.com`.
2. Activate the dedicated `a2ui-adb-pm-prod` gcloud configuration.
3. Confirm that the `FINANCIAL` application user exists in `paulparkdb` and
   that its password is known.
4. Confirm that the supply-chain database setup has been run in that schema.

Do not deploy with the `ADMIN` database user.

## Stage secrets

From PowerShell:

```powershell
cd a2ui_agui_mcpapps_mcptoolkit
.\deploy\gcp\prepare-secrets.ps1
```

The script packages `Wallet_PAULPARKDB`, prompts securely for the `FINANCIAL`
password, creates secret versions, and erases its temporary plaintext file.

## Build and deploy

For a first Gemini Enterprise demo, the agent card must be fetchable by the
host. This command creates an unauthenticated Cloud Run endpoint:

```powershell
.\deploy\gcp\deploy-cloud-run.ps1 -AllowUnauthenticated
```

Unauthenticated access is a demo setting, not the production target. Replace
it with Cloud Run IAM or OAuth authorization after the host validation.
The application still exposes only its bounded workflow: no general SQL tool,
wallet, password, Java port, or Toolkit transport is public.

Verify:

```powershell
$serviceUrl = gcloud run services describe oracle-supply-chain-a2ui `
  --project=adb-pm-prod --region=us-east4 `
  --format="value(status.url)"
Invoke-RestMethod "$serviceUrl/.well-known/agent-card.json"
```

## Register in Gemini Enterprise

After the live recommendation and approval paths pass:

```powershell
.\deploy\gcp\register-gemini-enterprise.ps1 `
  -AgentBaseUrl $serviceUrl
```

The registration script uses the official Discovery Engine
`assistants/default_assistant/agents` REST resource. It reads the deployed
agent card, checks A2A protocol `0.3.0`, refuses to create a duplicate display
name, and registers it in the existing `Inventory System` app.

## Source material

- [Google Cloud: Gemini Enterprise and A2UI integration guide](https://cloud.google.com/blog/topics/developers-practitioners/guide-to-gemini-enterprise-and-a2ui-integration)
- [Google Cloud: register and manage an A2UI agent](https://docs.cloud.google.com/gemini/enterprise/docs/a2ui-agents/register-and-manage-an-a2ui-agent)
- [Google Cloud: register and manage an A2A agent](https://docs.cloud.google.com/gemini/enterprise/docs/register-and-manage-an-a2a-agent)
- [Google A2UI source and examples](https://github.com/google/A2UI)
- [A2UI reference implementation linked by Google](https://github.com/wadave/agent-a2ui-demo)
- [Google Cloud: Direct VPC egress](https://docs.cloud.google.com/run/docs/configuring/vpc-direct-vpc)
- [Oracle: create an ODB network for Oracle Database@Google Cloud](https://docs.oracle.com/en-us/iaas/Content/database-at-gcp-autonomous/ogadb-task-1-creating-odb-network-oracle-databasegoogle.html)
- [Oracle: Oracle Database@Google Cloud network topologies](https://docs.oracle.com/en-us/iaas/Content/database-at-gcp/network-topologies.htm)
