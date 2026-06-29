---
name: create-pr
description: Open a GitHub pull request for the current work via the GitHub CLI (gh), following Tangem repo conventions — branch naming (feature/bugfix/AND-xxx), commit format (AND-xxx Description), base develop, required trailers. Picks which changes to include, creates a feature branch off a protected branch, commits, and — only after explicit confirmation — pushes and opens the PR. Use when the user asks to "open/create a PR", "создай ПР / пул-реквест", "open a pull request", "залей в PR".
allowed-tools: Read, Bash, AskUserQuestion, Monitor, TaskStop
argument-hint: [AND-xxxxx] [title...] [--base develop] [--dry-run]
---

Open a GitHub pull request for the current changes via `gh`, following this repo's conventions.

This skill is **interactive** and runs locally. **Pushing and opening the PR happen ONLY after an
explicit confirmation gate (Phase 4)** — never push or create the PR before the user confirms.

## Conventions

**Source of truth: [`.claude/rules/git-rules.md`](../../rules/git-rules.md)** — read it for branch
naming (`feature/`, `bugfix/`, **`tech/`**, `releases/`), the `AND-xxx Description` commit/PR-title
format, and the technical-PR exception (no Jira task → no `AND-xxx` in branch/commit/title). Do not
restate or fork those rules here; follow git-rules.md so this skill can't drift from it.

This skill only adds what is **not** in git-rules.md:

| Thing | Rule |
|---|---|
| Default PR base | `develop` (hotfix → the relevant `releases/x.xx`) |
| Protected branches | `develop`, `releases/*` — never commit directly; always branch off (Phase 2) |
| Commit trailer | `Co-Authored-By: Claude Opus 4.8 (1M context) <[REDACTED_EMAIL]>` |
| PR body footer | `🤖 Generated with [Claude Code](https://claude.com/claude-code)` |
| Code comments | **No `AND-xxx`** in code/KDoc (fine in branch/commit/PR) |

**Dry-run:** if `$ARGUMENTS` contains `--dry-run`, do everything except the writes — no branch
creation, no commit, no push, no `gh pr create`. Print the exact branch name, commit message, file
list, and `gh pr create` command that would run, then stop (see Phase 4D).

## Phase 0 — Preflight

Run these and stop with a clear FATAL message if any fails:

1. `gh auth status` — GitHub CLI must be authenticated. If not: `FATAL: gh is not authenticated. Run 'gh auth login'.`
2. `git rev-parse --abbrev-ref HEAD` — current branch. `git status --porcelain` — working tree.
3. `git remote get-url origin` and the repo's default branch (`gh repo view --json defaultBranchRef -q .defaultBranchRef.name`) for reference.

**Primary flow (default): branch + commit from existing local changes.** This skill takes the
**current uncommitted working-tree changes**, puts them on the right branch, commits, pushes, and
opens the PR. The target branch is decided by the **task** (Phase 1), not by whichever branch you
happen to be on:
- If the current branch is already the correct branch **for this task** (`feature/AND-xxxxx_…` /
  `bugfix/…` / `tech/…` matching the resolved task), commit the pending changes onto it.
- Otherwise — on a protected branch (`develop`/`releases/*`) **or on another task's feature branch** —
  create a new branch **off the base** (Phase 5 cuts it from `origin/<base>` so the other branch's
  commits don't ride along). Git keeps the uncommitted working-tree changes across this checkout.

Never leave local changes uncommitted and PR only what was already committed — the pending changes
are the point.

Fallback (no local changes): if `git status --porcelain` is empty **and** the current branch already
has commits ahead of the base that aren't PR'd, switch to a "PR an existing branch" flow — skip the
commit steps and go straight to push + PR. If the tree is empty and there are no un-PR'd commits
either, there is nothing to open a PR for — stop and say so.

## Phase 1 — Gather inputs

Parse `$ARGUMENTS` for an `AND-\d+` task id, a title, and `--base <branch>`. Ask only for what's
missing (use `AskUserQuestion` for constrained choices, plain text otherwise):

- **Task id** (`AND-xxxxx`) — **mandatory** for branch/commit/PR naming. **Always ask the user which
  task this PR is for** — never decide it silently. Every PR carries an `AND-xxxxx` **except** an
  explicit **Technical PR** (the one no-task exception, described below); do not offer a generic
  "no task / standalone" option outside that. You may pre-fill a *suggestion* (from `$ARGUMENTS`, or
  an `AND-\d+` found in the current branch name) as the recommended answer, but the user must confirm
  or override it. Do not assume the current branch's task id applies to the pending changes — they
  are often unrelated (e.g. you're on another task's branch). If the user gives no valid `AND-\d+`,
  keep asking — do not proceed without one.

  When asking, also offer a **"Create a new Jira Task"** option. If the user picks it, run the
  **`create-jira-task`** skill (it creates the Task from the local changes), then use the newly
  created `AND-xxxxx` as this PR's task id and continue. (Offer the Story-equivalent only if the work
  clearly warrants a Story; default to a Task.) **In `--dry-run`, do NOT actually run
  `create-jira-task`** — it's a real write; instead use a placeholder task id (e.g. `AND-NEW`) and
  note that the Task would be created.

  Also offer a **"Technical PR"** option (the one exception to the mandatory-task rule): a chore /
  tooling PR with **no Jira task**. If chosen, the change type becomes `tech`, the branch is
  `tech/<slug>` (no `AND-xxxxx`), and the commit subject + PR title have **no `AND-xxxxx` prefix**
  (just the plain English title).

  Options to present: the suggested existing key (if any), **Create a new Jira Task**, **Technical
  PR**, and free-text Other for an existing key. Outside of the Technical PR choice, never proceed
  without a valid `AND-\d+`.
- **Title** (English, required) — the PR/commit description. If absent, propose one generated from
  the staged/working changes (`git diff --stat`, `git log`) and ask the user to approve or edit.
  Must be English.
- **Change type** — `feature`, `bugfix`, or `tech` (drives the branch prefix). `tech` is set
  automatically when the user chose the **Technical PR** option above. Otherwise infer from the
  title/task; default `feature`.
- **Base branch** — default `develop`. Only change for hotfixes (`releases/x.xx`). Ask only if the
  current branch is itself a `releases/*` branch (then the base is likely that release line).
- **Files to include** — show `git status --porcelain` and let the user choose. Default to all
  tracked changes **except** unrelated submodule pointer bumps and stray edits; call out anything
  you exclude. If the user named specific files in `$ARGUMENTS` / the prompt (e.g. via `@path`),
  include exactly those.

## Phase 1b — Classify complexity & choose labels

Every PR gets exactly **one complexity label**. Count the **files chosen in Phase 1** (the planned
PR contents — not `git diff --cached`, since nothing is staged until Phase 5) and judge the nature of
the change. Propose a level
(via `AskUserQuestion`, recommending the one you judged) and let the user confirm or override:

| Label | Level | When | File limit |
|---|---|---|---|
| `deep` | 🔴 Red | Complex changes, or touching important/core logic | **≤ 15 files** |
| `complex` | 🟡 Yellow | Not deep and/or does not touch important core logic | **≤ 20 files** |
| `easy` | ⚪ White | Uniform/mechanical changes (rename, package move, formatting) | **no limit** |

Rules:
1. **Over the limit** → the PR body **must** include an explanation/justification of why the change
   could not be split or kept smaller. If the count exceeds the level's limit, ask the user for that
   justification and append it to the PR body under a `## Why this exceeds the <label> file limit`
   heading. Do not open an over-limit PR without it.
2. **Codeowner authority** — note in the PR body that the codeowner may request splitting the change
   or reject the PR. (Informational; nothing to enforce here.)
3. **Red (`deep`) PRs must have a description** — a non-empty, meaningful PR body explaining the
   change is mandatory (not just the summary line). If missing, ask the user for it before creating.
4. **Bug branches** — if the change type is `bugfix` (branch starts `bugfix/`), add the **`bug`**
   label **in addition** to the complexity label.

Resulting label set = the one complexity label (`deep`|`complex`|`easy`) + `bug` if it's a bugfix.

## Phase 2 — Derive the branch

- Build a slug from the title: lowercase, ASCII, spaces/punctuation → `_`, trimmed to ~5 words.
- Branch = `<feature|bugfix>/AND-xxxxx_<slug>` — or, for a Technical PR, `tech/<slug>` (no task id).
- **Reuse** the current branch only if it already matches **this task** (its `AND-xxxxx` / `tech`
  slug corresponds to the resolved task). Then commit onto it directly (no new branch).
- Otherwise **create a new branch off the base** — whether you're on a protected branch
  (`develop`/`releases/*`) **or on another task's feature branch**. Never commit to a protected
  branch, and never reuse an unrelated task's branch (its commits would ride into this PR).

## Phase 3 — Build the preview

Show everything that will happen, e.g.:

```
About to open a PR:

  Branch  : feature/AND-16023_jira_issue_creation_skills  (new, off develop)
  Base    : develop
  Files   :
    + .claude/skills/create-jira-story/SKILL.md
    + .claude/skills/create-jira-task/SKILL.md
  Excluded:
    ~ core/ui/ds-tokens   (unrelated submodule bump)

  Commit  : [REDACTED_TASK_KEY] Implement Jira Story/Task creation skills
  PR title: [REDACTED_TASK_KEY] Implement Jira Story/Task creation skills
  Labels  : complex            (2 files ≤ 20 — within limit)
  PR body : <first lines…>
```

Show the chosen labels, the file count vs. the level's limit, and — if over the limit — that a
justification is included. For a `bugfix` branch the line reads e.g. `Labels : deep, bug`.

## Phase 3b — Optional pre-PR checks (build / unit tests / detekt)

Before committing/pushing, ask via `AskUserQuestion` (**multiSelect**): **"Run any checks before
opening the PR?"** with options — **Build**, **Unit tests**, **Detekt**, **Skip all**. Run only what
the user picks. Prefer scoping to the affected modules when obvious; otherwise use the project-wide
commands from `CLAUDE.md`:

| Check | Command (project-wide) | Scoped example |
|---|---|---|
| Build | `./gradlew :app:assembleGoogleDebug` | `./gradlew :features:foo:impl:assembleDebug` |
| Unit tests | `./gradlew unitTest` | `./gradlew :features:foo:impl:testDebugUnitTest` |
| Detekt | `./gradlew detekt detektMain` | `./gradlew :features:foo:impl:detekt` |

Run the selected checks (long-running — use a generous timeout). Report each result.

- **All selected checks pass** → continue to Phase 4.
- **Any check fails** → show the failure output and ask how to proceed: **Fix first** (stop so the
  user / an appropriate agent can fix — e.g. `detekt-fixer` for detekt), **Open PR anyway** (proceed
  to Phase 4 despite the failure — note it in the PR body), or **Cancel**. Do **not** silently
  proceed past a failing check.

Skip this phase entirely in `--dry-run` mode (note in the dry-run output that checks were skipped).

## Phase 4D — Dry-run exit (when `--dry-run` is set)

Print the branch (and the `origin/<base>` it would be cut from), file list, commit message, chosen
labels (with the file-count-vs-limit check), and the literal `gh pr create` command that would run —
with **one `--label` flag per label** exactly as Phase 5 issues them, e.g.
`gh pr create --base <base> --head <branch> --label "<complexity>" [--label "bug"] --title "…" --body "…"`.
Make **no** changes (no branch, commit, push, or PR). Then stop.

## Phase 4 — Confirm (mandatory gate — covers the push)

Call `AskUserQuestion` "Create branch, commit, push, and open the PR?" with options:
- **Do it** — proceed to Phase 5.
- **Edit** — adjust branch/title/files/base, then re-preview.
- **Cancel** — stop; make no changes.

Do not run any write command (branch/commit/**push**/PR) until the user selects **Do it**. A previous
approval does not carry over to a later run.

## Phase 5 — Execute

1. **Branch** (if a new branch is needed — i.e. not reusing this task's branch): cut it **from the
   base**, not from the current HEAD, so another branch's commits don't ride along:
   `git fetch origin <base> && git checkout -b <branch> origin/<base>`. Git carries the uncommitted
   working-tree changes across the checkout. (When reusing this task's existing branch, skip this.)
2. **Stage**: `git add <selected paths>` — only the chosen files; never `git add -A` blindly.
3. **Commit** (skip if PR-ing an existing branch with no new changes):
   ```
   git commit -F- <<'EOF'
   AND-xxxxx <Title>

   <optional 1–3 line body>

   Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
   EOF
   ```
   For a **Technical PR**, the commit subject (and PR title) is just `<Title>` with **no `AND-xxxxx`
   prefix**.
4. **Push**: `git push -u origin <branch>`.
5. **PR**:
   ```
   gh pr create --base <base> --head <branch> \
     --label "<complexity label>" [--label "bug"] \
     --title "<commit subject>" \   # "AND-xxxxx <Title>", or just "<Title>" for a Technical PR (same as the commit subject)
     --body "$(cat <<'EOF'
   ## What
   <concise summary of the change>

   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   EOF
   )"
   ```
   Pass the complexity label (`deep`|`complex`|`easy`) via `--label`, plus a second `--label "bug"`
   for bugfix branches. `gh pr create` prints the PR URL on success. (If labels were missed at
   creation, add them after with `gh pr edit <url> --add-label "<label>"`.)

If a step fails, stop and surface the exact error and the command that failed; do not retry blindly.

## Phase 6 — Report

Output the PR URL, branch, base, the files included, and the labels. Do **not** offer to comment the
PR link on Jira or to change the Jira task status — those are out of scope for this skill.

## Phase 7 — Optional PR monitor

After the PR exists, ask via `AskUserQuestion`: **"Attach a monitor to this PR?"** (options: **Yes,
monitor** / **No**). If the user declines, stop.

If they accept, start a **persistent `Monitor`** that tracks the things worth acting on and emits one
line per occurrence:
- **Copilot inline review comments** (`pulls/{}/comments`).
- **Copilot conversation comments** (`issues/{}/comments`).
- **Copilot review summaries** (`pulls/{}/reviews` body) — the "## Pull request overview" text that
  is NOT an inline comment and would otherwise be missed.
- **Failed GitHub Actions checks** — especially **tests** and **detekt**, but report any failed check.

Comments/reviews are de-duplicated by **id** (a temp file), so the loop polls the full lists each
time without re-emitting; existing items are **seeded as already-seen** at startup so only genuinely
new activity is reported. Checks are de-duplicated per-poll (a re-failure on a new run still reports,
because the run goes through a `pending` phase that clears the set). Substitute the real
`<pr-number>` and `<owner/repo>`:

```
PR=<pr-number>; REPO=<owner/repo>
SEEN=$(mktemp)
# Seed existing Copilot comment/review ids as already-reported (only notify on NEW activity).
# Prefix by source (pc/ic/rv) so numeric ids from different endpoints can't collide.
{ gh api "repos/$REPO/pulls/$PR/comments"  --paginate --jq '.[]|"pc-\(.id)"' 2>/dev/null
  gh api "repos/$REPO/issues/$PR/comments" --paginate --jq '.[]|"ic-\(.id)"' 2>/dev/null
  gh api "repos/$REPO/pulls/$PR/reviews"               --jq '.[]|"rv-\(.id)"' 2>/dev/null; } >> "$SEEN" || true
report() {                          # stdin: "key<TAB>text"; emit unseen lines, persist their keys
  while IFS=$'\t' read -r key text; do
    [ -z "$key" ] && continue
    grep -qxF "$key" "$SEEN" && continue
    printf '%s\n' "$key" >> "$SEEN"
    printf '%s\n' "$text"
  done
}
seen_checks=""
while true; do
  { gh api "repos/$REPO/pulls/$PR/comments"  --paginate --jq '.[]|select(.user.login|test("[Cc]opilot"))|"pc-\(.id)\t💬 Copilot (inline \(.path)): \(.body|gsub("\n";" ")[0:200])"' 2>/dev/null || true; } | report
  { gh api "repos/$REPO/issues/$PR/comments" --paginate --jq '.[]|select(.user.login|test("[Cc]opilot"))|"ic-\(.id)\t💬 Copilot: \(.body|gsub("\n";" ")[0:200])"' 2>/dev/null || true; } | report
  { gh api "repos/$REPO/pulls/$PR/reviews"               --jq '.[]|select(.user.login|test("[Cc]opilot"))|select(.body!=null and .body!="")|"rv-\(.id)\t📝 Copilot review (\(.state)): \(.body|gsub("\n";" ")[0:200])"' 2>/dev/null || true; } | report
  # Failed checks — `gh pr checks` has no --json in older gh; parse TSV (col2 status). gh exits non-zero on failure (fine in $()).
  cur=$(gh pr checks "$PR" --repo "$REPO" 2>/dev/null | awk -F '\t' '$2=="fail"{print "❌ check failed: "$1}' | sort)
  comm -13 <(printf '%s\n' "$seen_checks") <(printf '%s\n' "$cur")
  seen_checks="$cur"
  sleep 60
done
```

Pass `persistent: true` and a specific `description` (e.g. `Copilot comments + failed checks on
AND-xxxxx PR #<n>`). Tell the user it runs for the session and can be stopped with `TaskStop`.