const titles = {
  reset: "Cold start confirmed",
  retain: "Memory retained through the SDK",
  recall: "Exact scoped recall completed",
  correct: "Durable fact corrected",
  expire: "TTL removed stale context",
  dream: "Trace pattern became a guideline",
  approve: "Human approval activated learning",
  "next-day": "Shared skill reused without leakage"
};

let lastNextDay = null;

document.querySelectorAll("[data-action]").forEach(button => {
  button.addEventListener("click", () => runAction(button.dataset.action, button));
});

async function runAction(action, button) {
  const buttons = [...document.querySelectorAll("[data-action]")];
  buttons.forEach(value => {
    value.disabled = true;
    value.classList.toggle("active", value === button);
  });
  setResult("Calling Oracle AI Agent Memory...", "The database transaction is in progress.");
  try {
    const response = await fetch(`/api/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}"
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || `HTTP ${response.status}`);
    if (action === "next-day") lastNextDay = result;
    if (action === "reset") lastNextDay = null;
    setResult(titles[action], result.message);
    if (result.contextCard) renderContext(result.contextCard);
    await loadState();
  } catch (error) {
    setResult("Action failed", error.message);
  } finally {
    buttons.forEach(value => value.disabled = false);
  }
}

function setResult(title, text) {
  document.getElementById("result-title").textContent = title;
  document.getElementById("result-text").textContent = text;
}

async function loadState() {
  const response = await fetch("/api/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "State unavailable");
  renderRecords(state.memories || []);
  renderTraces(state.traces || []);
  renderSkill(state.skills || []);
  renderEvents(state.events || []);
  if (state.contextCard) renderContext(state.contextCard);
  if (state.nextDay) lastNextDay = state.nextDay;
  if (state.latestStage) {
    setResult(titles[state.latestStage.stage] || state.latestStage.stage, state.latestStage.detail);
  }
  document.getElementById("memory-count").textContent = state.memories.length;
  document.getElementById("trace-count").textContent = state.traces.length;
  document.getElementById("skill-count").textContent = state.skills.length;
  document.getElementById("leak-count").textContent =
    lastNextDay ? lastNextDay.privateAvaMemoriesVisible : "?";
}

function renderContext(context) {
  document.getElementById("context-summary").textContent = context.summary;
  document.getElementById("context-response").textContent = context.response;
  const root = document.getElementById("context-list");
  const memories = context.relevantMemories || [];
  root.innerHTML = memories.length ? memories.map(value => `
    <div class="memory-chip"><b>${escapeHtml(value.recordType)}</b> ${escapeHtml(value.content)}</div>
  `).join("") : '<p class="empty">No eligible memory was recalled.</p>';
}

function renderRecords(records) {
  const root = document.getElementById("memory-list");
  root.innerHTML = records.length ? records.map(value => `
    <article class="record">
      <div class="tags">
        <span class="tag">${escapeHtml(value.recordType)}</span>
        <span class="tag">${escapeHtml(value.metadata.kind || "memory")}</span>
      </div>
      <p>${escapeHtml(value.content)}</p>
      <small>${escapeHtml(value.userId)} / ${escapeHtml(value.agentId)} · ${escapeHtml(value.id)}</small>
    </article>
  `).join("") : '<p class="empty">No Ava memories are currently searchable.</p>';
}

function renderTraces(traces) {
  const root = document.getElementById("trace-list");
  root.innerHTML = traces.length ? traces.map(value => `
    <div class="trace"><b>${escapeHtml(value.metadata.pattern)}</b><br>${escapeHtml(value.content)}</div>
  `).join("") : '<p class="empty">No governed traces.</p>';
}

function renderSkill(skills) {
  const root = document.getElementById("skill-card");
  const status = document.getElementById("skill-status");
  const skill = skills[0];
  if (!skill) {
    root.textContent = "No guideline has been induced.";
    status.textContent = "NONE";
    status.className = "";
    return;
  }
  const metadata = skill.metadata || {};
  status.textContent = (metadata.status || "pending").toUpperCase();
  status.className = metadata.status === "approved" ? "approved" : "";
  root.textContent = [
    `id: ${skill.id}`,
    `type: ${skill.recordType}`,
    `status: ${metadata.status}`,
    `source episodes: ${metadata.source_episode_count}`,
    `private guest data: ${metadata.private_guest_data_included}`,
    "",
    skill.content
  ].join("\n");
}

function renderEvents(events) {
  const root = document.getElementById("event-list");
  root.innerHTML = events.length ? events.slice().reverse().map(value => `
    <article class="event"><b>${escapeHtml(value.stage)}</b><small>${escapeHtml(value.detail)}</small></article>
  `).join("") : '<p class="empty">No actions yet.</p>';
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function initialize() {
  const status = document.getElementById("status");
  try {
    const response = await fetch("/api/health");
    const health = await response.json();
    if (!response.ok) throw new Error(health.error);
    status.classList.add("up");
    status.querySelector("span").textContent =
      `${health.sdk} · ${health.schema}@${health.database}`;
    document.getElementById("strategy").textContent = health.strategy;
    await loadState();
  } catch (error) {
    status.classList.add("down");
    status.querySelector("span").textContent = error.message;
    setResult("Connection failed", error.message);
  }
}

initialize();
