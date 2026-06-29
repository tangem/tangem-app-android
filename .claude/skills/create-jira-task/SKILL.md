---
name: create-jira-task
description: Create a Task in the Tangem Android Jira project (AND) via the Atlassian MCP. Pre-fills assignee (self), current active sprint, parent (Story or Epic), QA Notes, and other fields, asks the user for anything missing, shows a full preview, and creates the issue ONLY after explicit confirmation. Use when the user asks to "create a task", "создай задачу / таску в Jira", "заведи задачу", "open a Jira task".
allowed-tools: Read, Bash, mcp__claude_ai_Atlassian_Rovo__atlassianUserInfo, mcp__claude_ai_Atlassian_Rovo__getAccessibleAtlassianResources, mcp__claude_ai_Atlassian_Rovo__searchJiraIssuesUsingJql, mcp__claude_ai_Atlassian_Rovo__getJiraIssue, mcp__claude_ai_Atlassian_Rovo__createJiraIssue, mcp__claude_ai_Atlassian_Rovo__createIssueLink, AskUserQuestion
argument-hint: [summary text] [parent AND-xxxxx] [--dry-run]
---

Create a **Task** in the Tangem Android Jira project.

This skill is **interactive** — it runs locally for a developer, not on CI. Ask the user for any
missing data. **Never create the issue without an explicit confirmation step (Phase 4).**

**Dry-run mode:** if `$ARGUMENTS` contains `--dry-run`, do all the work (preflight, gather inputs,
resolve sprint, validate parent, build the preview and final payload) but **make no changes in
Jira** — skip the confirmation gate and the `createJiraIssue` call. See Phase 4D.

## Constants

| Key | Value |
|---|---|
| cloudId | `tangem.atlassian.net` |
| Project key | `AND` (quote as `"AND"` inside JQL — it collides with the `AND` keyword) |
| Issue type | `Task` (localized name `Задача`, id `10002`) |
| Issue browse URL | `https://tangem.atlassian.net/browse/{KEY}` |

### Field map (use these exact field IDs)

| Field | How to set | Notes |
|---|---|---|
| Summary | `summary` (top-level param) | **required** |
| Description | `description` (top-level param), `contentFormat: "markdown"` | optional but recommended |
| Issue type | `issueTypeName: "Task"` | fallback localized `"Задача"` if rejected |
| Assignee | `assignee_account_id` | **defaults to the current user** (self) |
| Sprint | `additional_fields: { "customfield_10021": <sprintId> }` | numeric id of the **active** sprint |
| Stream | `additional_fields: { "customfield_11931": { "id": "<optionId>" } }` | optional single-select (see option ids below) |
| Parent | hierarchy `parent` accepts an **Epic only** | Story/Task/Bug are all the same hierarchy level, so a **Story can NOT be the `parent` of a Task** (Jira rejects it: "parent does not belong to the hierarchy"). **Epic parent** → set `parent: "<epic>"`. **Story parent** → set the hierarchy `parent` to the **Story's own parent Epic** (inherit it, so the Task lands in the same Epic) **and** create the Phase 5b "implements" link to the Story. If the Story has no parent Epic, omit `parent` and rely on the link only. |
| QA Notes | `additional_fields: { "customfield_11232": <ADF doc> }` | **ADF only** — a plain string is rejected ("must be an Atlassian document"). Wrap the text: `{"type":"doc","version":1,"content":[{"type":"paragraph","content":[{"type":"text","text":"<text>"}]}]}` |
| Story Points | `additional_fields: { "customfield_10025": <number> }` | optional |
| Developer | `additional_fields: { "customfield_11898": { "accountId": "<id>" } }` | optional |
| Labels | `additional_fields: { "labels": ["..."] }` | optional |
| Components | `additional_fields: { "components": [{ "name": "..." }] }` | optional |

### Stream option ids (single-select, optional)

If the user picks a Stream, map the chosen value to its option **id** (preferred); on a `{ "id": ... }`
rejection retry that field with `{ "value": "<value>" }`.

| Value | id | | Value | id |
|---|---|---|---|---|
| `Core` | `15117` | | `Visa` | `15981` |
| `Blockchain` | `15120` | | `Engagement` | `15118` |
| `Grow` | `15116` | | `App store` | `15119` |

> **Tool names:** the phases below reference MCP tools by short name (e.g. `createJiraIssue`,
> `getAccessibleAtlassianResources`) for readability. These map to the fully-qualified Atlassian Rovo
> tools declared in `allowed-tools` (`mcp__claude_ai_Atlassian_Rovo__*`) — the connected server for
> this skill. Invoke them by their fully-qualified names.

## Phase 0 — Preflight

1. Verify the Atlassian MCP is reachable: call `getAccessibleAtlassianResources` (no params). If it
   fails or `tangem.atlassian.net` is absent, STOP with:
   `FATAL: Atlassian MCP is not connected. Run 'claude mcp list' to check server status.`
2. Determine the current user (the default **assignee** / self):
   - If the current user's Jira `accountId` is already known from memory/prior context, **reuse it
     and skip the API call** — it is stable and does not change.
   - Otherwise call `atlassianUserInfo`, save `account_id` and `name`, and remember it for next time
     (so future runs skip this call).

## Phase 1 — Gather inputs

Parse `$ARGUMENTS` for an obvious summary and/or a parent key (`AND-\d+`). Then collect the rest.
Ask the user **only for what is still missing**, grouped into as few questions as possible
(use `AskUserQuestion` where the choice is constrained, plain text otherwise):

- **Summary** (required) — short task title. **Must be in English** (mandatory). If it was not provided in `$ARGUMENTS`,
  ask the user (via `AskUserQuestion`): **"Generate the summary from your local changes?"** with
  options:
  - **Generate from local changes** — inspect the working tree and derive a concise English title
    from it: run `git status --porcelain`, `git diff --stat HEAD`, `git branch --show-current` (and
    `git log --oneline -5` for context). Propose a one-line summary and show it to the user for
    approval/editing before using it. If there are no local changes, say so and fall back to asking
    for the title manually.
  - **Enter manually** — ask the user to type the title.
- **Description** — **do NOT ask the developer for it; generate it yourself.** Write a short
  **2–3 sentence** summary of *what* the change is and *why* (**English or Russian** are both
  acceptable; default to English), derived from the local changes
  (the same `git` inspection used for the summary) and the chosen title. It must be a concise digest,
  **not** a full listing of every local change, file, or diff. Show the generated description in the
  Phase 3 preview so the developer can approve or edit it before creation.
- **Parent** (optional) — a Story or Epic. Ask via `AskUserQuestion` with these options:
  - **No parent** — proceed without one (omit the `parent` field).
  - **Provide a link/key** — the user knows the parent. When this option is chosen, **ask in a
    separate follow-up message** for the link or `AND-xxxxx` key. Then extract the key (`AND-\d+`)
    from whatever the user pastes (a plain key or a full `browse/AND-...` URL).
  - **Search by keyword** — the user doesn't know the key; ask for a keyword and run
    `searchJiraIssuesUsingJql` with
    `jql: 'project = "AND" AND issuetype IN (Story, Epic) AND statusCategory != Done AND summary ~ "<kw>" ORDER BY updated DESC'`, then let them pick. **Sanitize `<kw>` before interpolating** — escape `\` and `"` (and drop other JQL metacharacters) so the keyword can't break or alter the query; if it can't be safely escaped, ask the user to rephrase.
- **QA Notes** — testing notes for QA. **Must be strictly in Russian.** Optional. Ask via
  `AskUserQuestion`. The option **labels are in English**, but the value written to the QA Notes
  field stays in Russian:
  - **Nothing to test** → write the exact Russian text `Ничего тестировать не нужно`.
  - **Test within the story** → testing happens within the parent Story; write the Russian text
    `Тестируйте в рамках стори`. If the change is gated behind a feature toggle (detect a toggle name
    from the local changes / context, e.g. a new entry in `feature_toggles_config.json` or an
    `XxxFeatureToggles` usage), append it: `Тестируйте в рамках стори, закрыто тогглом "<название>"`.
    If no toggle is found, use the plain text without the toggle clause.
  - **Generate from changes** → generate QA Notes (in Russian) from the local changes, written for
    **testers**: describe the user-facing behaviour to verify in plain language, **without any
    code-level names** (no class / component / function / file names). You may include concrete
    **test cases** (step → expected result). If you know the feature toggle that gates this
    functionality (detect it from the local changes / context — e.g. a new entry in
    `feature_toggles_config.json` or an `XxxFeatureToggles` usage), add `Закрыто тогглом "<название>"`.
    Show the generated text in the Phase 3 preview for approval/editing.
  - **Enter manually** → let the user type custom QA Notes (in Russian).
  - **No QA Notes** → leave the field empty.
- **Stream** (optional) — ask via `AskUserQuestion` offering the values from the **Stream option ids**
  table plus a **Skip (no Stream)** option (last). `Core` is the common default for most work; pick a
  specific stream only when it clearly applies. If the user skips, omit the field. Map the chosen
  value to its option id.
- **Story Points** — only ask if the user mentions it or hints at it; otherwise skip silently.
- **labels / components / Developer** — **never ask** for these. Only set them if the user provided
  them explicitly in `$ARGUMENTS`; otherwise omit them entirely.

Validate any provided parent key with `getJiraIssue` (fields `["summary","issuetype","parent"]`) so
the preview can show the parent's title, confirm it exists, and read its own parent. **Use the
parent's `issuetype` to decide what goes where:**
- **Epic** → the Epic is the hierarchy `parent` (Phase 5) *and* the target of the Phase 5b link.
- **Story** → the Task **inherits the Story's parent Epic** as its hierarchy `parent` (so it lands in
  the same Epic), and the Phase 5b "implements" link points to the **Story**. Read the Story's Epic
  from its `parent` field (the `getJiraIssue` above; if absent there, fetch the Story with
  `fields:["parent"]`). If the Story has **no** parent Epic, omit the `parent` field and rely on the
  link only.
- **Any other type** (Task, Bug, Sub-task, …) → a non-Epic, non-Story key is **not** a valid parent
  here: it can't be a hierarchy `parent` (same hierarchy level) and isn't an "implements" target for a
  Task. Warn the user that the key is a `<issuetype>` and ask (via `AskUserQuestion`) whether to
  **provide an Epic/Story key instead** or **proceed with no parent** (omit `parent` and skip the
  Phase 5b link). Never silently treat it as an Epic or Story.

Track two distinct values: **`linkTarget`** (the chosen parent — Story or Epic, used for the Phase 5b
link) and **`hierarchyParent`** (the Epic that goes into the `parent` field — the Epic itself, or the
Story's inherited Epic, or none). Reflect both in the preview, e.g.
`Parent: [REDACTED_TASK_KEY] (Story, link) — hierarchy Epic [REDACTED_TASK_KEY] (inherited)` or
`Parent: [REDACTED_TASK_KEY] (Epic, hierarchy + link)`.

## Phase 2 — Resolve the current sprint

Run:
```
searchJiraIssuesUsingJql
  jql: 'project = "AND" AND sprint IN openSprints() ORDER BY updated DESC'
  fields: ["customfield_10021"]
  maxResults: 50
```
Inspect the `customfield_10021` arrays across the returned issues (each is an array of sprint
objects — a single `maxResults: 1` issue could belong only to a future open sprint). Collect the
distinct sprint with `state == "active"`; use its `id` (number) for the Sprint field and its `name`
for the preview.

If no active sprint is found, tell the user and ask whether to create the Task **without** a sprint
or to supply a sprint id manually. Never guess a sprint id.

## Phase 3 — Build the preview

Render a compact table of every field that WILL be sent, e.g.:

```
About to create a Jira TASK in project AND:

  Summary      : <summary>
  Type         : Task
  Assignee     : <self name> (you)
  Sprint       : Mobile Sprint 208 (id 4181)
  Stream       : Core (or "—" if skipped)
  Parent       : [REDACTED_TASK_KEY] — Wallet registration (Story, link) — hierarchy Epic [REDACTED_TASK_KEY] (inherited)
  QA Notes     : <text or "—">
  Story Points : <n or "—">
  Description  :
    <first ~5 lines, or "—">
```

Show empty fields as `—` so the developer sees exactly what is and isn't set.

## Phase 4D — Dry-run exit (when `--dry-run` is set)

If `$ARGUMENTS` contains `--dry-run`, do **not** ask for confirmation and do **not** call
`createJiraIssue`. Instead, after the Phase 3 preview, print the exact `createJiraIssue` payload that
*would* be sent (all params and `additional_fields`), prefixed with a clear banner:

```
DRY RUN — no Jira issue was created. Payload that would be sent:
```

If a parent was set, also note the **issue link** that would be created after the issue:
`createIssueLink type="Polaris work item link", inwardIssue=<new Task>, outwardIssue=<linkTarget>`
(reads "<linkTarget> is implemented by <new Task>"), and — for a Story parent — that the hierarchy
`parent` would be the Story's inherited Epic. Then stop — this is the full extent of the run in
dry-run mode.

## Phase 4 — Confirm (mandatory gate)

(Skipped entirely in dry-run mode — see Phase 4D.)

Call `AskUserQuestion` with the question "Create this Jira Task?" and options:
- **Create** — proceed to Phase 5.
- **Edit fields** — go back to Phase 1 and adjust the field(s) the user names, then re-preview.
- **Cancel** — stop; create nothing.

Do not call `createJiraIssue` until the user selects **Create**.

## Phase 5 — Create

Call `createJiraIssue`:
```
cloudId: "tangem.atlassian.net"
projectKey: "AND"
issueTypeName: "Task"
summary: "<summary>"
description: "<description>"            # omit if empty
contentFormat: "markdown"
assignee_account_id: "<self account_id>"
parent: "<hierarchyParent>"            # the Epic (chosen Epic, or the Story's inherited Epic); omit if none
additional_fields: {
  "customfield_10021": <sprintId>,      # omit if no sprint
  "customfield_11931": { "id": "<streamOptionId>" },   # Stream — omit if skipped
  # QA Notes — ADF document, NOT a plain string (omit if empty):
  "customfield_11232": {"type":"doc","version":1,"content":[{"type":"paragraph","content":[{"type":"text","text":"<QA Notes>"}]}]},
  "customfield_10025": <points>         # omit if not set
}
```
For multi-line QA Notes, use one `paragraph` per line inside the ADF `content` array.

**Fallbacks** (retry once, only on a field-specific error):
- Issue type rejected → retry with `issueTypeName: "Задача"`.
- Stream (`customfield_11931`) rejected for `{ "id": ... }` → retry that field with `{ "value": "<value>" }`.
- `parent` rejected with "does not belong to the hierarchy" → `hierarchyParent` should already be an
  Epic, so this is unexpected (e.g. a non-Epic slipped through). Drop the `parent` field and create
  the Task without it; the relationship is still carried by the Phase 5b "implements" link.
- `parent` (an Epic) rejected for another reason → drop `parent` and set
  `additional_fields.customfield_10014: "<epic>"` (legacy Epic Link).
- A single custom field rejected → report which field failed and ask whether to retry without it.

## Phase 5b — Link the new Task to its parent (only if a parent was set)

After the Task is created, if a parent was provided, create an **issue link** to **`linkTarget`** (the
chosen parent — the Story, or the Epic) so it **reads "is implemented by <new Task>"** (the new Task
*implements* its parent). Note: the hierarchy `parent` set in Phase 5 is the **Epic** (`hierarchyParent`),
while this link points to the **chosen parent** — for a Story parent those are two different issues
(the Task sits under the Story's Epic *and* implements the Story).

```
createIssueLink
  cloudId: "tangem.atlassian.net"
  type: "Polaris work item link"     # the implements / is implemented by link type
  inwardIssue: "<new issue key>"     # new Task → "implements" the parent
  outwardIssue: "<linkTarget>"       # chosen parent (Story or Epic) → "is implemented by" the new Task
```

If the link call fails, do **not** treat the whole run as failed (the Task is already created) —
report the error and the link parameters so it can be added manually.

## Phase 6 — Report

On success, output the new issue key, summary, and URL
`https://tangem.atlassian.net/browse/{KEY}`, plus a one-line recap of the set fields (sprint, parent,
assignee) and — if a parent was set — confirm the **"<parent> is implemented by <new issue>"** link was created.
On failure, surface the API error message verbatim and the field values you attempted.