# Agent Toolkit — approach & contents

A system for building **continuable, orchestratable** Claude Code subagents, with an
Android agent set and an analyzer to keep agents healthy.

## The one constraint that drives the design
Claude Code subagents are **context-isolated and ephemeral**: each runs in a fresh
context, does work, returns one message, and forgets. They can't see the parent
conversation or each other. Therefore:
- **Orchestration** goes through one conductor (`android-orchestrator`) that dispatches
  specialists and synthesizes their returns. Specialists never talk to each other.
- **Context lives on disk**, not in chat. The root `CLAUDE.md` is the project's
  architecture overview (modules, layers, dependency rules, entry points) — every agent
  reads it on entry.

## The contract every agent follows
- **Entry:** read the root `CLAUDE.md` before doing anything.
- **Exit:** return the `HANDOFF` block (asked / did / state / impact / blockers / next /
  how-to-verify).

This contract is the whole answer to "a user can resume at any time with minimal effort":
each HANDOFF block makes its step legible cold, so the orchestrator (and a human) can
synthesize where things stand and what to do next.

## Contents
```
agent-toolkit/
  README.md            ← this file (the approach)
  RUBRIC.md            ← 10-dimension agent quality spec
  analyze_agents.py    ← dependency-free linter that scores agents against the rubric
  templates/
    HANDOFF.md         ← return-contract template
~/.claude/agents/
  android-orchestrator.md        ← conductor: plans, dispatches, synthesizes HANDOFFs
  android-feature-builder.md     ← implements within the architecture
  android-code-reviewer.md       ← Android-pitfall correctness review (read-only)
  android-build-test.md          ← Gradle build/test, iterate to green
  android-architecture-guardian.md ← enforces boundaries & layering
  agent-auditor.md               ← meta-agent: audits/improves other agents via RUBRIC.md
```

## Usage
- **Start Android work:** invoke `android-orchestrator` with your goal.
- **Audit agents (tooling):** `python3 ~/.claude/agent-toolkit/analyze_agents.py`
- **Audit agents (judgment):** invoke `agent-auditor` for substance-level review + fixes.

## Extending to other stacks
The pattern is stack-agnostic. Clone the android-* set, swap the domain checklists
(build commands, framework pitfalls) in each specialist, keep the orchestrator,
contracts, and rubric unchanged.