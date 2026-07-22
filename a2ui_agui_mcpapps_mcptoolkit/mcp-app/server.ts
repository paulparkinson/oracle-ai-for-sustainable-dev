import { readFile } from "node:fs/promises";
import path from "node:path";
import cors from "cors";
import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { registerAppResource, registerAppTool, RESOURCE_MIME_TYPE } from "@modelcontextprotocol/ext-apps/server";
import { z } from "zod";

const resourceUri = "ui://oracle-account-risk/dashboard-v1";
const agentServiceUrl = process.env.AGENT_SERVICE_URL ?? "http://127.0.0.1:8080";
const AccountSchema = z.object({
  customerId: z.number().int().positive(),
  customerName: z.string(),
  industry: z.string(),
  accountValue: z.number(),
  riskScore: z.number(),
  riskLevel: z.string(),
  riskSummary: z.string(),
  ownerName: z.string(),
  followUpStatus: z.string()
});
const GovernedAccountsSchema = z.object({
  source: z.literal("oracle-db-mcp-java-toolkit"),
  accounts: z.array(AccountSchema)
});

async function loadGovernedAccounts(minimumRisk: number, maximumRows: number) {
  const endpoint = new URL("/api/accounts", agentServiceUrl);
  endpoint.searchParams.set("minimumRisk", String(minimumRisk));
  endpoint.searchParams.set("maximumRows", String(maximumRows));
  const response = await fetch(endpoint, { signal: AbortSignal.timeout(15_000) });
  if (!response.ok) throw new Error(`Governed account request failed with HTTP ${response.status}`);
  return GovernedAccountsSchema.parse(await response.json()).accounts;
}

const server = new McpServer({ name: "Oracle Account Risk MCP App", version: "0.1.0" });
registerAppTool(server, "show-account-risk-dashboard", {
  title: "Show account risk dashboard",
  description: "Shows Oracle Database MCP Java Toolkit-governed account-risk data as an interactive dashboard.",
  inputSchema: z.object({
    minimumRisk: z.number().min(0).max(100).default(75).describe("Minimum governed risk score"),
    maximumRows: z.number().int().min(1).max(50).default(10).describe("Maximum governed accounts to display")
  }),
  _meta: { ui: { resourceUri, visibility: ["model", "app"] } }
}, async ({ minimumRisk, maximumRows }) => {
  const accounts = await loadGovernedAccounts(minimumRisk, maximumRows);
  return {
    content: [{ type: "text", text: `Oracle Database MCP Java Toolkit returned ${accounts.length} governed accounts.` }],
    structuredContent: { accounts, source: "oracle-db-mcp-java-toolkit", minimumRisk, maximumRows }
  };
});

registerAppResource(server, resourceUri, resourceUri, { mimeType: RESOURCE_MIME_TYPE }, async () => ({
  contents: [{ uri: resourceUri, mimeType: RESOURCE_MIME_TYPE, text: await readFile(path.join(import.meta.dirname, "dist", "mcp-app.html"), "utf8") }]
}));

const app = express();
app.use(cors({ origin: false }));
app.use(express.json({ limit: "256kb" }));
app.get("/health", (_request, response) => response.json({ status: "UP", dataSource: agentServiceUrl }));
app.post("/mcp", async (request, response) => {
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined, enableJsonResponse: true });
  response.on("close", () => transport.close());
  await server.connect(transport);
  await transport.handleRequest(request, response, request.body);
});
app.listen(3001, "127.0.0.1", () => console.log("MCP App server listening on http://127.0.0.1:3001/mcp"));
