# Agent Quality Rubric

A scoring spec for Claude Code subagents (`.claude/agents/*.md`). Each dimension is
scored **0 (absent) / 1 (partial) / 2 (solid)**. Max score = 20.

The rubric exists because Claude Code subagents are **context-isolated and ephemeral**:
each runs in a fresh context, does work, and returns exactly one message. They cannot
see the parent conversation or each other. Most agent-quality problems trace back to
authors forgetting this. The rubric is built to catch those problems.

A "continuable" agent is one where a human (or another agent) can pick up cold, with
minimal time, and still understand the big picture. Dimensions 4–6 protect that property.

---

## Dimensions

### 1. Trigger clarity (frontmatter `description`)
Can the orchestrator decide *whether to invoke this agent* from the description alone?
- **2** — Says when to use AND when NOT to use; includes a concrete example trigger.
- **1** — Says when to use, but no negative guidance or examples.
- **0** — Vague ("helps with code") or missing.

### 2. Tool scoping (frontmatter `tools`)
Least privilege. A read-only analyzer must not hold `Write`/`Edit`.
- **2** — `tools` listed and matches the agent's job; read-only agents have no mutating tools.
- **1** — `tools` listed but broader than needed.
- **0** — No `tools` field (silently inherits everything), or obvious over-grant.

### 3. Single responsibility
One clear job. Agents that "do everything" can't be orchestrated or audited.
- **2** — One crisp mandate; explicitly defers adjacent work to other agents.
- **1** — Mostly focused but with scope creep.
- **0** — Grab-bag of unrelated duties.

### 4. Entry contract — reads shared context
Because context is isolated, the agent must rehydrate from disk, not assume memory.
- **2** — Explicitly reads the root `CLAUDE.md` (or named inputs) as step one.
- **1** — Reads some context but not the project's architecture overview.
- **0** — Assumes it already knows the project; no entry read.

### 5. Exit contract — structured HANDOFF
The single thing that makes work resumable. Output must be legible cold.
- **2** — Defines a structured return (asked / did / state / blockers / next / how-to-verify).
- **1** — Returns a summary but unstructured.
- **0** — No defined output shape.

### 6. Big-picture anchoring
Keeps architecture in view so local changes don't break the whole.
- **2** — Reasons against the architecture in `CLAUDE.md`; flags structural impact in its HANDOFF.
- **1** — Mentions architecture but doesn't tie decisions to it.
- **0** — Purely local; no architectural awareness.

### 7. Guardrails & escalation
Knows its limits and stop conditions.
- **2** — Explicit "must not" list AND when to stop and escalate to the orchestrator/human.
- **1** — Some guardrails, no escalation path (or vice versa).
- **0** — None.

### 8. Self-verification
Tells how its own output should be checked.
- **2** — Concrete verification (run these tests / this build / these checks).
- **1** — Says "verify" without specifics.
- **0** — None.

### 9. Determinism of process
A repeatable procedure, not vibes.
- **2** — Numbered, ordered steps the agent follows every run.
- **1** — Loose guidance.
- **0** — Freeform.

### 10. Conciseness & specificity
No filler; concrete over abstract.
- **2** — Tight, every line earns its place, concrete nouns/paths.
- **1** — Some bloat or vague phrasing.
- **0** — Long, generic, or contradictory.

---

## Score bands
- **18–20** — Production-ready. Orchestratable and continuable.
- **13–17** — Usable; fix the 0/1 dimensions.
- **8–12** — Risky; likely breaks under orchestration or loses context.
- **0–7** — Rewrite.

## How to use
- Script: `python3 ~/.claude/agent-toolkit/analyze_agents.py <path-or-glob>`
- Meta-agent: invoke `agent-auditor` — it reads this rubric and proposes concrete edits.