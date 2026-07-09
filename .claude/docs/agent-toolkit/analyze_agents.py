#!/usr/bin/env python3
"""
analyze_agents.py — grade Claude Code subagents against RUBRIC.md.

Heuristic, dependency-free linter. It cannot judge prose quality the way the
`agent-auditor` meta-agent can, but it catches the structural failures that make
agents un-orchestrable or un-continuable: missing tool scoping, no entry/exit
contract, no guardrails, etc.

Usage:
    python3 analyze_agents.py                      # scan ./.claude/agents and ~/.claude/agents
    python3 analyze_agents.py path/to/agent.md     # one file
    python3 analyze_agents.py 'dir/*.md'           # a glob
    python3 analyze_agents.py --json               # machine-readable
"""
import sys
import os
import re
import glob
import json

# Each check returns (score 0..2, message). Mirrors RUBRIC.md dimensions.

MUTATING_TOOLS = {"write", "edit", "notebookedit", "multiedit"}
READONLY_NAME_HINTS = ("review", "audit", "analyz", "inspect", "explore",
                       "cartograph", "map", "guardian", "lint", "check")


def parse_agent(text):
    """Split frontmatter from body. Returns (meta, body).

    Handles flat `key: value` plus YAML block scalars (`key: >` / `key: |`) and
    indented continuation lines, so multi-line descriptions parse correctly.
    """
    meta, body = {}, text
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", text, re.DOTALL)
    if not m:
        return meta, body
    raw, body = m.group(1), m.group(2)
    lines = raw.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip() or line.lstrip().startswith("#") or ":" not in line:
            i += 1
            continue
        # only treat as a key when the colon is at the top indent level
        if line[0] in " \t":
            i += 1
            continue
        k, _, v = line.partition(":")
        key, v = k.strip().lower(), v.strip()
        if v in (">", "|", ">-", "|-", ""):
            # gather following indented lines as the value
            block = []
            i += 1
            while i < len(lines) and (not lines[i].strip() or lines[i][:1] in " \t"):
                block.append(lines[i].strip())
                i += 1
            meta[key] = " ".join(b for b in block if b).strip()
        else:
            # strip one layer of matching surrounding quotes, e.g. tools: "Read, Edit"
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "\"'":
                v = v[1:-1]
            meta[key] = v
            i += 1
    return meta, body


def has_any(text, *words):
    low = text.lower()
    return any(w in low for w in words)


def check_trigger(meta, body, name):
    desc = meta.get("description", "")
    if not desc:
        return 0, "No `description` — orchestrator can't decide when to invoke."
    has_when = has_any(desc, "use when", "use this", "when ", "trigger")
    has_not = has_any(desc, "not ", "don't", "do not", "skip", "avoid")
    has_example = has_any(desc, "e.g.", "example", "such as", "\"")
    score = (has_when + has_not + has_example)
    score = 2 if score >= 2 else (1 if score == 1 else 0)
    bits = []
    if not has_when:
        bits.append("add explicit 'use when ...'")
    if not has_not:
        bits.append("add 'do NOT use for ...'")
    if not has_example:
        bits.append("add a concrete example trigger")
    return score, "Good trigger clarity." if score == 2 else "; ".join(bits)


def check_tools(meta, body, name):
    tools = meta.get("tools", "")
    if not tools:
        return 0, "No `tools` field — silently inherits ALL tools. Scope it."
    toolset = {t.strip().lower() for t in re.split(r"[,\s]+", tools) if t.strip()}
    readonly_named = any(h in name.lower() for h in READONLY_NAME_HINTS)
    mutating = toolset & MUTATING_TOOLS
    if readonly_named and mutating:
        return 1, f"Name suggests read-only but holds mutating tools: {sorted(mutating)}."
    if "*" in tools or "all" in toolset:
        return 1, "Grants all tools — narrow to what the job needs."
    return 2, "Tools are scoped."


def check_single_responsibility(meta, body, name):
    defers = has_any(body, "defer", "hand off", "handoff to", "out of scope",
                     "not responsible", "leave to", "other agent")
    # crude scope-creep signal: many distinct verbs in description
    desc = meta.get("description", "").lower()
    verbs = sum(desc.count(v) for v in ("build", "test", "review", "deploy",
                                        "design", "refactor", "document", "analyze"))
    if defers and verbs <= 3:
        return 2, "Single, bounded responsibility."
    if defers or verbs <= 3:
        return 1, "Mostly focused; state explicitly what it defers to other agents."
    return 0, "Looks like a grab-bag — split it or define one mandate."


def check_entry(meta, body, name):
    reads_context = has_any(body, "claude.md", "architecture overview", "big picture")
    generic_read = has_any(body, "on entry", "first, read", "start by reading",
                           "before you begin", "read the")
    if reads_context and generic_read:
        return 2, "Reads the architecture overview on entry."
    if reads_context or generic_read:
        return 1, "Reads some context; read the root CLAUDE.md as step one."
    return 0, "No entry read — will assume context it doesn't have (isolation bug)."


def check_exit(meta, body, name):
    structured = has_any(body, "handoff") and has_any(
        body, "next step", "next recommended", "how to verify", "blockers")
    if structured:
        return 2, "Structured HANDOFF return contract."
    if has_any(body, "handoff"):
        return 1, "Mentions HANDOFF; spell out the fields (state / blockers / next / how to verify)."
    return 0, "No exit contract — output won't be resumable."


def check_big_picture(meta, body, name):
    architecture = has_any(body, "claude.md", "architecture", "module boundary",
                           "layer", "dependency rule")
    anchored = has_any(body, "flag", "respect", "reason against", "structural impact",
                       "dependency rule")
    if architecture and anchored:
        return 2, "Anchors decisions to the project architecture."
    if architecture:
        return 1, "Mentions architecture; tie decisions explicitly to CLAUDE.md."
    return 0, "No big-picture anchoring."


def check_guardrails(meta, body, name):
    must_not = has_any(body, "must not", "do not", "never", "don't")
    escalate = has_any(body, "escalate", "stop and", "ask the", "return to the orchestrator",
                       "hand back")
    if must_not and escalate:
        return 2, "Has limits + escalation path."
    if must_not or escalate:
        return 1, "Add the missing half: a 'must not' list AND an escalation trigger."
    return 0, "No guardrails or stop conditions."


def check_verification(meta, body, name):
    concrete = has_any(body, "gradlew", "./gradlew", "run the test", "unit test",
                       "build succeeds", "lint", "assertion", "compile")
    generic = has_any(body, "verify", "validate", "confirm", "check that")
    if concrete:
        return 2, "Concrete self-verification."
    if generic:
        return 1, "Says verify but no concrete method."
    return 0, "No self-verification."


def check_determinism(meta, body, name):
    numbered = len(re.findall(r"^\s*\d+[\.\)]\s+", body, re.MULTILINE))
    if numbered >= 3:
        return 2, "Has an ordered procedure."
    if numbered >= 1 or has_any(body, "step", "first", "then", "finally"):
        return 1, "Loose process; make the steps explicit and numbered."
    return 0, "No defined procedure."


def check_conciseness(meta, body, name):
    words = len(body.split())
    vague = sum(body.lower().count(p) for p in (
        "as needed", "appropriate", "etc.", "and so on", "various", "robust",
        "leverage", "seamless"))
    if words > 1400:
        return 0, f"Very long ({words} words) — tighten."
    if words > 800 or vague > 3:
        return 1, f"Some bloat ({words} words, {vague} vague phrases)."
    return 2, f"Tight ({words} words)."


CHECKS = [
    ("Trigger clarity", check_trigger),
    ("Tool scoping", check_tools),
    ("Single responsibility", check_single_responsibility),
    ("Entry contract", check_entry),
    ("Exit contract", check_exit),
    ("Big-picture anchoring", check_big_picture),
    ("Guardrails & escalation", check_guardrails),
    ("Self-verification", check_verification),
    ("Determinism", check_determinism),
    ("Conciseness", check_conciseness),
]


def band(score):
    if score >= 18:
        return "PRODUCTION-READY"
    if score >= 13:
        return "USABLE"
    if score >= 8:
        return "RISKY"
    return "REWRITE"


def analyze_file(path):
    with open(path, encoding="utf-8") as f:
        text = f.read()
    meta, body = parse_agent(text)
    name = meta.get("name", os.path.basename(path).rsplit(".", 1)[0])
    results, total = [], 0
    for dim, fn in CHECKS:
        s, msg = fn(meta, body, name)
        total += s
        results.append({"dimension": dim, "score": s, "note": msg})
    return {"path": path, "name": name, "total": total,
            "band": band(total), "checks": results}


def discover(args):
    targets = [a for a in args if not a.startswith("-")]
    if targets:
        files = []
        for t in targets:
            files.extend(glob.glob(os.path.expanduser(t)) if any(c in t for c in "*?[")
                         else [os.path.expanduser(t)])
        return [f for f in files if f.endswith(".md")]
    files = []
    for d in (".claude/agents", os.path.expanduser("~/.claude/agents")):
        files.extend(sorted(glob.glob(os.path.join(d, "*.md"))))
    return files


def print_report(reports):
    for r in reports:
        print(f"\n{'='*68}\n{r['name']}  —  {r['total']}/20  [{r['band']}]\n{r['path']}\n{'-'*68}")
        for c in r["checks"]:
            mark = {0: "✗", 1: "~", 2: "✓"}[c["score"]]
            print(f"  {mark} {c['dimension']:<26} {c['score']}/2  {c['note']}")
    if len(reports) > 1:
        print(f"\n{'='*68}\nSUMMARY")
        for r in sorted(reports, key=lambda x: x["total"]):
            print(f"  {r['total']:>2}/20  [{r['band']:<16}] {r['name']}")


def main():
    args = sys.argv[1:]
    files = discover(args)
    if not files:
        print("No agent .md files found. Pass a path/glob, or run where "
              ".claude/agents exists.", file=sys.stderr)
        sys.exit(1)
    reports = [analyze_file(f) for f in files]
    if "--json" in args:
        print(json.dumps(reports, indent=2))
    else:
        print_report(reports)


if __name__ == "__main__":
    main()
