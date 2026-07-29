const cues = {
  reset: {
    step: "00",
    title: "Begin at a cold start",
    text: "An agent without external memory re-onboards the user every time. Reset proves there is no hidden browser state carrying this demo."
  },
  retain: {
    step: "01",
    title: "Retain the experience, not the whole transcript",
    text: "The write path extracts useful facts, an episode, and a temporary operational note. Each record has an identity scope, type, source, version, and lifecycle."
  },
  recall: {
    step: "02",
    title: "Read before the next turn",
    text: "Recall filters by Ava, the concierge agent, active status, and expiry. It returns a compact context card that the response can reuse."
  },
  correct: {
    step: "03",
    title: "Corrections are first-class data",
    text: "Ava says lantern show, not fireworks. The transaction supersedes the old fact and activates a new version, preserving an auditable history."
  },
  expire: {
    step: "04",
    title: "Not every memory should live forever",
    text: "The rain-route closure is operational memory with a short TTL. Expiring it prevents stale state from influencing tomorrow."
  },
  dream: {
    step: "05",
    title: "Traces become learning material",
    text: "Three successful, privacy-safe traces share a pattern. The demo deterministically induces a candidate procedure in token space; it does not retrain model weights."
  },
  approve: {
    step: "06",
    title: "Learning remains governed",
    text: "A person reviews and approves the proposed skill. Only then can it enter the active procedural-memory path."
  },
  "next-day": {
    step: "07",
    title: "Transfer the lesson, not the private memory",
    text: "Leo benefits from the approved rainy-evening workflow. The scope proof remains zero: none of Ava's private facts are visible."
  }
};

const actionBodies = {
  recall: { guestId: "AVA", query: "Build a rain-safe evening plan" },
  approve: { approver: "demo.presenter@example.com" }
};

let latestContext = null;
let latestNextDay = null;

document.querySelectorAll("[data-action]").forEach(button => {
  button.addEventListener("click", () => runAction(button.dataset.action, button));
});

async function runAction(action, button) {
  setActive(button, action);
  const controls = [...document.querySelectorAll("[data-action]")];
  controls.forEach(control => control.disabled = true);
  showResult("Working with Oracle AI Database…");
  try {
    const response = await fetch(`/api/actions/${action}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(actionBodies[action] || {})
    });
    const value = await response.json();
    if (!response.ok) throw new Error(value.error || `HTTP ${response.status}`);
    if (action === "reset") {
      latestContext = null;
      latestNextDay = null;
    }
    if (action === "recall") latestContext = value.contextCard;
    if (action === "next-day") latestNextDay = value;
    showResult(value.message || "Step completed.");
    await loadState();
  } catch (error) {
    showResult(error.message, true);
  } finally {
    controls.forEach(control => control.disabled = false);
  }
}

function setActive(button, action) {
  document.querySelectorAll("[data-action]").forEach(value => value.classList.remove("active"));
  button.classList.add("active");
  applyCue(action);
}

function applyCue(action) {
  const cue = cues[action];
  document.getElementById("step-number").textContent = cue.step;
  document.getElementById("cue-title").textContent = cue.title;
  document.getElementById("cue-text").textContent = cue.text;
}

async function loadState() {
  const response = await fetch("/api/state");
  const state = await response.json();
  if (!response.ok) throw new Error(state.error || "Unable to load demo state");
  renderMemories(state.memories || []);
  renderTraces(state.traces || []);
  renderSkills(state.skills || []);
  if (state.latestStage) applyCue(state.latestStage);
  if (state.nextDay) latestNextDay = state.nextDay;
  const activeMemories = (state.memories || []).filter(memory =>
    memory.status === "ACTIVE" &&
    (!memory.expiresAt || new Date(memory.expiresAt) > new Date())
  );
  if (activeMemories.length) {
    latestContext = {
      summary: "Returning guest who values quiet mornings and accessible routes.",
      relevantMemories: activeMemories.map(memory => ({
        type: memory.type,
        content: memory.content
      })),
      proposedResponse:
        "Use a quiet early meal and the covered accessible route, then use the confirmed lantern-show preference."
    };
  }
  renderContext();
  renderScopeProof(state.skills || []);
  if (state.latestStage === "next-day") {
    showResult(state.nextDay.message);
  }
}

function renderMemories(memories) {
  const root = document.getElementById("memory-ledger");
  document.getElementById("memory-count").textContent =
    `${memories.length} record${memories.length === 1 ? "" : "s"}`;
  if (!memories.length) {
    root.innerHTML = '<p class="empty">The durable ledger is empty.</p>';
    return;
  }
  root.innerHTML = memories.map(memory => `
    <article class="memory-row">
      <header>
        <span class="tag">${escapeHtml(memory.type)}</span>
        <span class="tag ${memory.status.toLowerCase()}">${escapeHtml(memory.status)}</span>
        <b>${escapeHtml(memory.key)} · v${memory.version}</b>
      </header>
      <p>${escapeHtml(memory.content)}</p>
      <div class="memory-meta">${escapeHtml(memory.guestId)} / ${escapeHtml(memory.scope)} · ${escapeHtml(memory.source)}${memory.expiresAt ? ` · expires ${shortTime(memory.expiresAt)}` : ""}</div>
    </article>
  `).join("");
}

function renderTraces(traces) {
  const root = document.getElementById("trace-list");
  document.getElementById("trace-count").textContent =
    `${traces.length} trace${traces.length === 1 ? "" : "s"}`;
  if (!traces.length) {
    root.innerHTML = '<p class="empty">No traces yet.</p>';
    return;
  }
  root.innerHTML = traces.slice().reverse().map(trace => `
    <article class="trace">
      <b>${escapeHtml(trace.asked)}</b>
      <small>${escapeHtml(trace.action)} → ${escapeHtml(trace.outcome)}</small>
      ${trace.pattern ? `<small>Pattern: ${escapeHtml(trace.pattern)}</small>` : ""}
    </article>
  `).join("");
}

function renderSkills(skills) {
  const skill = skills.at(-1);
  const status = document.getElementById("skill-status");
  const card = document.getElementById("skill-card");
  if (!skill) {
    status.textContent = "NONE";
    status.className = "status-pill";
    card.textContent = "No skill has been proposed.";
    return;
  }
  status.textContent = skill.status;
  status.className = `status-pill ${skill.status.toLowerCase()}`;
  let instructions = skill.instructions;
  try { instructions = JSON.stringify(JSON.parse(instructions), null, 2); } catch (_) {}
  card.textContent =
    `name: ${skill.name}\nstatus: ${skill.status}\nsource episodes: ${skill.sourceEpisodes}\ntrigger: ${skill.trigger}\n\n${instructions}`;
}

function renderContext() {
  const list = document.getElementById("context-memories");
  if (!latestContext) {
    document.getElementById("context-summary").textContent = "No recall has run yet.";
    document.getElementById("proposed-response").textContent =
      "The response is assembled only after the read step.";
    list.innerHTML = '<li class="empty">Active, unexpired, correctly scoped memories will appear here.</li>';
    return;
  }
  document.getElementById("context-summary").textContent = latestContext.summary;
  document.getElementById("proposed-response").textContent = latestContext.proposedResponse;
  list.innerHTML = latestContext.relevantMemories.map(memory =>
    `<li><b>${escapeHtml(memory.type)}</b> · ${escapeHtml(memory.content)}</li>`
  ).join("");
}

function renderScopeProof(skills) {
  const approved = skills.find(skill => skill.status === "APPROVED");
  document.getElementById("leak-count").textContent =
    latestNextDay ? latestNextDay.privateAvaMemoriesVisible : "-";
  document.getElementById("shared-skill").textContent = approved
    ? `Shared and approved: ${approved.name}. Its instructions contain no private guest data.`
    : "No approved shared skill yet.";
}

function showResult(message, error = false) {
  const root = document.getElementById("result");
  root.textContent = message;
  root.classList.toggle("error", error);
}

function shortTime(value) {
  try { return new Date(value).toLocaleString(); } catch (_) { return value; }
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
  const status = document.getElementById("db-status");
  try {
    const response = await fetch("/api/health");
    const health = await response.json();
    if (!response.ok) throw new Error(health.error);
    status.classList.add("up");
    status.querySelector("span").textContent =
      `${health.schema}@${health.database} · ${health.pool}`;
    await loadState();
  } catch (error) {
    status.classList.add("down");
    status.querySelector("span").textContent = error.message || "Database unavailable";
    showResult(error.message || "Database unavailable", true);
  }
}

initialize();
