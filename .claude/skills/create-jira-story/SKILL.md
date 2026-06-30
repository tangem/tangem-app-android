---
name: create-jira-story
description: Create a feature Story in the Tangem Android Jira project (AND) via the Atlassian MCP. Pre-fills assignee (self), current active sprint, parent (Epic), QA Notes, and other fields, asks the user for anything missing, shows a full preview, and creates the issue ONLY after explicit confirmation. Use when the user asks to "create a story", "создай историю / стори в Jira", "заведи фичу", "open a Jira story".
allowed-tools: Read, Bash, mcp__claude_ai_Atlassian_Rovo__atlassianUserInfo, mcp__claude_ai_Atlassian_Rovo__getAccessibleAtlassianResources, mcp__claude_ai_Atlassian_Rovo__searchJiraIssuesUsingJql, mcp__claude_ai_Atlassian_Rovo__getJiraIssue, mcp__claude_ai_Atlassian_Rovo__createJiraIssue, mcp__claude_ai_Atlassian_Rovo__createIssueLink, AskUserQuestion
argument-hint: [summary text] [parent AND-xxxxx] [--dry-run]
---

Create a **feature Story** in the Tangem Android Jira project.

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
| Issue type | `Story` (localized name `История`, id `10001`) |
| Issue browse URL | `https://tangem.atlassian.net/browse/{KEY}` |

### Field map (use these exact field IDs)

| Field | How to set | Notes |
|---|---|---|
| Summary | `summary` (top-level param) | **required** |
| Description | `description` (top-level param), `contentFormat: "markdown"` | optional but recommended |
| Issue type | `issueTypeName: "Story"` | fallback localized `"История"` if rejected |
| Assignee | `assignee_account_id` | **defaults to the current user** (self) |
| Sprint | `additional_fields: { "customfield_10021": <sprintId> }` | numeric id of the **active** sprint |
| Stream | `additional_fields: { "customfield_11931": { "id": "<optionId>" } }` | optional single-select (see option ids below) |
| Parent (Epic) | `parent: "AND-xxxxx"` (top-level param) | Story parent is normally an Epic; fallback `customfield_10014` (Epic Link) |
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

- **Summary** (required) — short feature title. **Must be in English** (mandatory). If it was not provided in `$ARGUMENTS`,
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
- **Parent Epic** (optional) — ask via `AskUserQuestion` with these options:
  - **No parent** — proceed without one (omit the `parent` field).
  - **Provide a link/key** — the user knows the Epic. When this option is chosen, **ask in a
    separate follow-up message** for the Epic link or `AND-xxxxx` key. Then extract the key
    (`AND-\d+`) from whatever the user pastes (a plain key or a full `browse/AND-...` URL).
  - **Search by keyword** — the user doesn't know the key; ask for a keyword and run
    `searchJiraIssuesUsingJql` with
    `jql: 'project = "AND" AND issuetype = Epic AND statusCategory != Done AND summary ~ "<kw>" ORDER BY updated DESC'`, then let them pick. **Sanitize `<kw>` before interpolating** — escape `\` and `"` (and drop other JQL metacharacters) so the keyword can't break or alter the query; if it can't be safely escaped, ask the user to rephrase.
- **QA Notes** — testing notes for QA. **Must be strictly in Russian.** Optional. Ask via
  `AskUserQuestion`. The option **labels are in English**, but the value written to the QA Notes
  field stays in Russian:
  - **Nothing to test** → write the exact Russian text `Ничего тестировать не нужно`.
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

Validate any provided parent key with `getJiraIssue` (fields `["summary","issuetype"]`) so the
preview can show the parent's title and confirm it exists. Then track two distinct values:
- **`linkTarget`** — the chosen parent, used for the Phase 5b "implements" link.
- **`hierarchyParent`** — the Epic that goes into the `parent` field. A Story's only valid hierarchy
  parent is an **Epic**: if the chosen parent **is** an Epic, `hierarchyParent` = that Epic. If it is
  **not** an Epic (e.g. a Story/Task), warn the user (a Story can't sit under a non-Epic) and either
  ask for an Epic key or set `hierarchyParent` = none — the relationship is then carried by the
  Phase 5b link alone.

So the normal case (Epic parent) sets both to the same Epic; a non-Epic parent sets `linkTarget` only.

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

If no active sprint is found, tell the user and ask whether to create the Story **without** a sprint
or to supply a sprint id manually. Never guess a sprint id.

## Phase 3 — Build the preview

Render a compact table of every field that WILL be sent, e.g.:

```
About to create a Jira STORY in project AND:

  Summary       : <summary>
  Type          : Story
  Assignee      : <self name> (you)
  Sprint        : Mobile Sprint 208 (id 4181)
  Stream        : Core (or "—" if skipped)
  Parent        : [REDACTED_TASK_KEY] — Android refactoring (Epic, hierarchy + link)
                  [or "[REDACTED_TASK_KEY] — Foo (Story, link only — no hierarchy parent)"]
  QA Notes      : <text or "—">
  Story Points  : <n or "—">
  Description   :
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

If a `linkTarget` was chosen, also note the **issue link** that would be created after the issue:
`createIssueLink type="Polaris work item link", inwardIssue=<new Story>, outwardIssue=<linkTarget>`
(reads "<linkTarget> is implemented by <new Story>"), and whether the hierarchy `parent`
(`hierarchyParent`) is set or omitted. Then stop — this is the full extent of the run in dry-run mode.

## Phase 4 — Confirm (mandatory gate)

(Skipped entirely in dry-run mode — see Phase 4D.)

Call `AskUserQuestion` with the question "Create this Jira Story?" and options:
- **Create** — proceed to Phase 5.
- **Edit fields** — go back to Phase 1 and adjust the field(s) the user names, then re-preview.
- **Cancel** — stop; create nothing.

Do not call `createJiraIssue` until the user selects **Create**.

## Phase 5 — Create

Call `createJiraIssue`:
```
cloudId: "tangem.atlassian.net"
projectKey: "AND"
issueTypeName: "Story"
summary: "<summary>"
description: "<description>"            # omit if empty
contentFormat: "markdown"
assignee_account_id: "<self account_id>"
parent: "<hierarchyParent>"            # the Epic; omit if none (non-Epic parent or no parent)
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
- Issue type rejected → retry with `issueTypeName: "История"`.
- Stream (`customfield_11931`) rejected for `{ "id": ... }` → retry that field with `{ "value": "<value>" }`.
- `hierarchyParent` (an Epic) rejected → drop `parent`, set `additional_fields.customfield_10014: "<epic>"` (legacy Epic Link).
- A single custom field rejected → report which field failed and ask whether to retry without it.

## Phase 5b — Link the new Story to its parent (only if a `linkTarget` was chosen)

After the Story is created, if a `linkTarget` was chosen, create an **issue link** to it so it
**reads "is implemented by <new Story>"** (the new Story *implements* its parent). When
`hierarchyParent` was also set (the normal Epic case), this complements the hierarchy `parent` field;
when only a `linkTarget` was chosen (non-Epic parent, no hierarchy parent), this link is the sole
relationship.

```
createIssueLink
  cloudId: "tangem.atlassian.net"
  type: "Polaris work item link"     # the implements / is implemented by link type
  inwardIssue: "<new issue key>"     # new Story → "implements" the parent
  outwardIssue: "<linkTarget>"       # chosen parent → "is implemented by" the new Story
```

If the link call fails, do **not** treat the whole run as failed (the Story is already created) —
report the error and the link parameters so it can be added manually.

## Phase 6 — Report

On success, output the new issue key, summary, and URL
`https://tangem.atlassian.net/browse/{KEY}`, plus a one-line recap of the set fields (sprint, parent,
assignee) and — if a parent was set — confirm the **"<parent> is implemented by <new issue>"** link was created.
On failure, surface the API error message verbatim and the field values you attempted.