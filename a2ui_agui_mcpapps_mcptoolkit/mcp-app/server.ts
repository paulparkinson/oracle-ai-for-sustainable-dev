import { readFile } from "node:fs/promises";
import path from "node:path";
import cors from "cors";
import express from "express";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { registerAppResource, registerAppTool, RESOURCE_MIME_TYPE } from "@modelcontextprotocol/ext-apps/server";

const resourceUri = "ui://oracle-account-risk/dashboard-v1";
const accounts = [
  { customerId: 1, customerName: "Apex Freight Systems", accountValue: 4200000, riskScore: 96, riskLevel: "CRITICAL", riskSummary: "Payment velocity and beneficiary changes exceed policy." },
  { customerId: 2, customerName: "Blue Mesa Energy", accountValue: 8100000, riskScore: 93, riskLevel: "CRITICAL", riskSummary: "Sanctions-screening similarity and unusual cross-border payments." },
  { customerId: 4, customerName: "Delta Retail Group", accountValue: 2850000, riskScore: 88, riskLevel: "HIGH", riskSummary: "Chargeback spike and new settlement account." }
];

const server = new McpServer({ name: "Oracle Account Risk MCP App", version: "0.1.0" });
registerAppTool(server, "show-account-risk-dashboard", {
  title: "Show account risk dashboard",
  description: "Shows governed account-risk data as an interactive dashboard.",
  inputSchema: {},
  _meta: { ui: { resourceUri, visibility: ["model", "app"] } }
}, async () => ({
  content: [{ type: "text", text: `Account risk dashboard contains ${accounts.length} accounts.` }],
  structuredContent: { accounts }
}));

registerAppResource(server, resourceUri, resourceUri, { mimeType: RESOURCE_MIME_TYPE }, async () => ({
  contents: [{ uri: resourceUri, mimeType: RESOURCE_MIME_TYPE, text: await readFile(path.join(import.meta.dirname, "dist", "mcp-app.html"), "utf8") }]
}));

const app = express();
app.use(cors({ origin: false }));
app.use(express.json({ limit: "256kb" }));
app.post("/mcp", async (request, response) => {
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined, enableJsonResponse: true });
  response.on("close", () => transport.close());
  await server.connect(transport);
  await transport.handleRequest(request, response, request.body);
});
app.listen(3001, "127.0.0.1", () => console.log("MCP App server listening on http://127.0.0.1:3001/mcp"));
