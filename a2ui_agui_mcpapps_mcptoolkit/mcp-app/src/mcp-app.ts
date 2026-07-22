import { App } from "@modelcontextprotocol/ext-apps";

type Account = { customerId: number; customerName: string; accountValue: number; riskScore: number; riskLevel: string; riskSummary: string };
const app = new App({ name: "Oracle Account Risk Dashboard", version: "0.1.0" });
const metrics = document.querySelector<HTMLDivElement>("#metrics")!;
const accountsElement = document.querySelector<HTMLDivElement>("#accounts")!;
const sourceElement = document.querySelector<HTMLParagraphElement>("#source")!;

app.ontoolresult = (result) => {
  const payload = result.structuredContent as { accounts?: Account[]; source?: string; minimumRisk?: number } | undefined;
  sourceElement.textContent = payload?.source === "oracle-db-mcp-java-toolkit"
    ? `Live governed results from the Oracle Database MCP Java Toolkit · minimum risk ${payload.minimumRisk}`
    : "Waiting for governed Toolkit results.";
  render(payload?.accounts ?? []);
};
app.connect();

function render(accounts: Account[]) {
  metrics.replaceChildren(); accountsElement.replaceChildren();
  if (accounts.length === 0) {
    const empty = document.createElement("p"); empty.textContent = "No accounts matched the governed risk threshold.";
    accountsElement.append(empty); return;
  }
  const total = accounts.reduce((sum, account) => sum + account.accountValue, 0);
  const critical = accounts.filter(account => account.riskLevel === "CRITICAL").length;
  [
    ["Accounts", String(accounts.length)],
    ["Critical", String(critical)],
    ["Value at risk", new Intl.NumberFormat(undefined, { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(total)]
  ].forEach(([label, value]) => metrics.append(metric(label, value)));
  accounts.forEach(account => accountsElement.append(accountCard(account)));
}

function metric(label: string, value: string) {
  const box = document.createElement("div"); box.className = "metric";
  const strong = document.createElement("strong"); strong.textContent = value;
  const span = document.createElement("span"); span.textContent = label;
  box.append(strong, span); return box;
}

function accountCard(account: Account) {
  const box = document.createElement("article"); box.className = "account";
  const name = document.createElement("strong"); name.textContent = account.customerName;
  const score = document.createElement("div"); score.textContent = `${account.riskLevel} · ${account.riskScore}`;
  const bar = document.createElement("div"); bar.className = "bar";
  const fill = document.createElement("span"); fill.style.width = `${Math.min(100, Math.max(0, account.riskScore))}%`; bar.append(fill);
  const summary = document.createElement("p"); summary.textContent = account.riskSummary;
  const button = document.createElement("button"); button.textContent = "Send selection to conversation";
  button.addEventListener("click", () => app.updateModelContext({ content: [{ type: "text", text: `Selected account ${account.customerId}: ${account.customerName}` }] }));
  box.append(name, score, bar, summary, button); return box;
}
