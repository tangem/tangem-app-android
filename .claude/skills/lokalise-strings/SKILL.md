---
name: lokalise-strings
compatibility: Needs the Lokalise MCP server, python3 with `python-lokalise-api` for the pull script, and `lokalise.project.id` / `lokalise.token` present in `local.properties`.
description: Use when adding, changing, or deprecating user-facing localized strings in this app — new UI text, string resources, Lokalise keys, R.string / R.plurals, TextReference, strings.xml under core/res, plurals, placeholders, missing translations. Not for `values/strings_special.xml` (`translatable="false"`, outside Lokalise — edit it directly), nor for non-localized debug/log text. Триггеры: «добавь/заведи строку», «завести ключ в локалайзе», «поменяй текст», «нужна строка для экрана».
allowed-tools: Read, Grep, Glob, Edit, Bash, AskUserQuestion, mcp__lokalise_sd__list_lokalise_keys, mcp__lokalise_sd__get_lokalise_key, mcp__lokalise_sd__create_lokalise_keys, mcp__lokalise_sd__update_lokalise_key
---

# Lokalise Strings

## Overview

The single source of truth for all user-facing strings is the Lokalise project **"App"** (`project_id: 4965953963bd330202ba50.61798973`), shared between iOS and Android. Base language is `en`; the other 10 languages are translated by the loc team through their own process.

The files `core/res/src/main/res/values*/strings.xml` are a **generated export** from Lokalise. Never edit them by hand — they are only pulled via `python3 lokalize.py`. Exception: `values/strings_special.xml` (`translatable="false"`, outside Lokalise).

**Violating the letter of these rules is violating their spirit.** Workarounds (hand-editing XML, raw REST API, self-made translations) are forbidden even when they look faster.

## Quick reference

| What | Value |
|---|---|
| project_id | `4965953963bd330202ba50.61798973` |
| Create keys | `mcp__lokalise_sd__create_lokalise_keys` (bulk, one call per feature) |
| Check for existing keys | grep `values/strings.xml` + `mcp__lokalise_sd__list_lokalise_keys` (`filter_keys` — exact names, comma-separated) |
| Mark as deprecated | `mcp__lokalise_sd__update_lokalise_key`, `tags: ["Deprecated"]`, `merge_tags: true` |
| `key_name` | plain string (`per_platform_key_names=false`), snake_case |
| `platforms` | `["ios", "android"]` for a new key. When creating a **replacement** for an existing key, copy that key's platforms instead — some carry `other`, which drives the loc team's `%LANG_ISO%.csv` export, and silently dropping it removes the string from that feed |
| Key names & tags | agent proposes — **the user approves**; never create unconfirmed |
| Translations at creation | **`en` only** |
| Pull into the repo | `python3 lokalize.py` — **all languages**, no `--langs` filter (needs `lokalise.project.id` and `lokalise.token` in `local.properties`; `pip3 install --user python-lokalise-api==4.1.0`, add `--break-system-packages` only if that is refused) |

## Workflow: adding a string

1. **Check for an existing key — search by the English TEXT, not just by the key name you have in mind.**
   - `grep -niE '>[^<]*optional[^<]*<' core/res/src/main/res/values/strings.xml` for each string. Generic one-word
     labels usually already exist under a *different* feature prefix — real examples: `send_optional_field` =
     "Optional", `onramp_country_unavailable` = "Unavailable".
   - Confirm in Lokalise with `list_lokalise_keys`: `filter_keys` = your candidate names plus the reuse candidates,
     `filter_archived: "include"`, `include_translations: 1`.
   - Read the `tags` of every candidate you found: a key tagged `Deprecated` is on its way out, so
     never reuse it — treat it as absent and create a new key instead.
   - **A `common_*` hit means reuse it.** That cluster (244 keys) is shared by design —
     `common_cancel` is referenced from 17 modules, `common_continue` from 13 — so a generic label
     already living there needs no new key and no question. Just use it.
   - A hit under **another feature's** prefix (`send_*`, `onramp_*`, …) is different: reusing it
     couples your screen to that team's wording. Put **reuse vs. a new prefixed key** to the user as
     an explicit choice in step 4.
2. **Draft key names**: `<feature>_<screen>_<element>`, snake_case. Prefix — same as the neighboring key cluster of that screen (grep; e.g. `tangempay_current_plan_*`). Never rename existing keys.
3. **Draft the EN text in universal-placeholder format** (table below). Take the format from that table, NOT from the repo XML — the XML contains the already-exported Android format.
   - **Leave decorative punctuation out of the key.** A user asking for « · Optional» is describing the
     rendering, not the string: the key holds `Optional`, and the ` · ` separator is appended in Compose
     (precedent: `append(" · ")` at 5 call sites). Same for leading dashes, bullets, colons and
     surrounding quotes — they are layout.
   - The exception is a **format template that joins arguments**, where the separator is part of the
     string because neither argument owns it, and translators localize the separator too (ru and ja
     use `・`, uk uses `.`). Precedent `tangem_pay_history_item_spend_mc_title_format`: Lokalise holds
     **`[%s] · [%s]`** — brackets included, per the placeholder table. Do not copy the *exported*
     Android form (`%s · %s`) into Lokalise: unbracketed placeholders break the iOS export. That key
     is also legacy in using two *unpositioned* `[%s]`, which is why Android has to export it with
     `formatted="false"`; for a new key with 2+ arguments use numbered `[%1$s]` / `[%2$s]` as the
     table requires.
4. **Confirm with the user — mandatory gate.** Show the proposed key names (or at least the key prefix) and tags, and wait for approval before creating anything; in an interactive session use AskUserQuestion. Exception: if the user already specified the key names / prefix / tags in the task, that IS the approval — don't re-ask.
5. **Create the keys** in one call:

```json
mcp__lokalise_sd__create_lokalise_keys
{
  "project_id": "4965953963bd330202ba50.61798973",
  "keys": [{
    "key_name": "tangempay_current_plan_cashback_description",
    "platforms": ["ios", "android"],
    "description": "Current Plan screen. [%1$s] = cashback percent (2), [%2$s] = formatted limit ($500).",
    "translations": [{
      "language_iso": "en",
      "translation": "You get [%1$s][%] cashback on purchases up to [%2$s]"
    }]
  }]
}
```

   Fill `description` when the string has arguments or non-obvious context — translators read it. `tags`: only the ones the user approved in step 4 (omit the field if none). Don't set `use_automations`, `filenames`, `is_hidden`. Check `errors` in the response (partial failures; "key already exists" → don't recreate, reuse the existing key).
6. **Pull into the repo**: `python3 lokalize.py`. Without this step `R.string.<key>` doesn't exist and the build fails.
   - **Check the tree first.** The pull *overwrites* all 11 `values*/strings.xml` (~2.5 MB) with no
     merge and no backup: any uncommitted edit in them is destroyed silently. `git status` those
     paths, and commit or stash before pulling.
   - **No `--langs` filter — pull every language.** `en` alone is enough to compile, but the other 10
     `values-*/strings.xml` are tracked too and go stale otherwise; the surrounding commits (`[REDACTED_TASK_KEY]`,
     `[REDACTED_TASK_KEY]`, `[REDACTED_TASK_KEY]`, …) all carry locale files.
   - Locale files show up in the diff only when translations actually changed, so an `en`-only *diff* is normal —
     an `en`-only *pull* is not.
   - Pipe the output (`| tail -4`): the script echoes the API token on startup, and it must not land in the transcript.
7. **`git diff core/res`**: commit **every** file the pull touched — the base `values/strings.xml` and each
   `values-*` locale.
   - Do **not** strip other teams' lines. The export is shared generated state; carrying it along is what keeps the
     repo current and stops keys getting lost.
   - **Read the deletions before committing them.** The pull removes lines as well as adding them
     (a routine pull here deleted 45), and a key archived by mistake upstream disappears from the
     repo the same way a deliberate one does. `git diff -- core/res | grep '^-  <'` and satisfy
     yourself each removal is intended; a deleted key still referenced from Kotlin fails the build,
     but one referenced only from a layout or a locale fails silently at runtime.
   - Check that **your** keys are actually in the diff. Missing means the pull didn't export them — re-check
     `errors` from step 5 and that the `en` translation is set (untranslated keys are skipped).
8. **Usage**: in UM/Model — `resourceReference(R.string.x, wrappedList(...))` / `pluralReference(...)`; in Compose — `stringResourceSafe` / `pluralStringResourceSafe` (raw `stringResource`/`pluralStringResource` are banned by the `UnsafeStringResourceUsage` detekt rule).

## Universal placeholders (mandatory in Lokalise)

No bare `%s`, `%d`, `%@` in texts destined for Lokalise — square brackets only. One format exports to both Android (`%s`) and iOS (`%@`).

| In Lokalise | Android export | When |
|---|---|---|
| `[%s]` | `%s` | the only string argument |
| `[%1$s]`, `[%2$s]` | `%1$s`, `%2$s` | 2+ arguments — numbered only |
| `[%d]` | `%d` | integers (counters, plurals) |
| `[%]` | `%` with no arguments / `%%` with arguments | literal percent |

Example: `APR [%1$s][%]` → Android `APR %1$s%%` (live key `staking_apr_earn_badge`). Optionally add a translator hint after a colon — `[%1$s:percent]`, `[%2$s:limit]`; the hint is visible to translators in Lokalise but stripped from exports (live key: `tangempay_current_plan_fee_charged_notification` = `[%1$s:fee] monthly fee will be charged on [%2$s:date]`). Non-breaking space — insert the actual NBSP character (U+00A0) into the text (example: `common_terms_of_use`).

## Plurals

`"is_plural": true`, the translation is an object of plural forms:

```json
{
  "key_name": "tangempay_current_plan_days_left",
  "platforms": ["ios", "android"],
  "is_plural": true,
  "translations": [{
    "language_iso": "en",
    "translation": { "one": "[%d] day left", "other": "[%d] days left" }
  }]
}
```

At the call site the count goes in **twice** — `pluralReference(id, count = n, formatArgs = wrappedList(n))`.
`count` only picks the one/other form; `formatArgs` defaults to empty, so omitting it selects the right
wording and then renders no number at all.

Two different suffixes, two different reasons — do not mix them. `_v2` replaces a key whose
**argument contract** changed and retires the original with `Deprecated`. `_android` splits a key
whose **platform wording** diverges; both keys stay alive, each owning one platform, and nothing is
deprecated.

If the Android wording must differ from iOS, split it off rather than editing the shared key. The live
precedent is `manage_tokens_number_of_wallets_android`: `_android` suffix, `platforms: ["android"]`,
tag `android-specific`, and a description pointing at its iOS twin. The original key keeps
`platforms: ["ios"]` so each platform owns its own text. What matters is the **platforms** field —
`common_days` is android-only with no suffix at all — but keep the suffix for a key that has a twin,
so the pair is obvious in the Lokalise UI.

Only `en` is ever authored, even for languages with more plural forms than English: `common_days`
ships one/few/many/other in `values-ru` off a two-form `en` source. The loc team fills the rest — do
not invent `few`/`many`.

## Changing an existing string

**Treat changing the text of an existing key as a DANGEROUS operation.** Never do it as a side
effect of another task. Three checks come first, before you touch anything:

1. **Is it already released?** Compare against the newest release branch:
   `git show origin/releases/<x.y>:core/res/src/main/res/values/strings.xml | grep '"<key>"'`
   (pick the branch with `git branch -r | grep -oE "origin/releases/[0-9]+\.[0-9]+$" | sort -V | tail -1`).
   A hit means shipped clients already carry this wording.
2. **Is the feature actually live?** A key can sit in a release branch behind a *closed* toggle,
   in which case no user has ever seen the wording. Find the toggle gating the call site and look it
   up in `core/config-toggles/src/main/assets/configs/feature_toggles_config.json` (a flat
   `[{name, version}]` list): `"version": "undefined"` means never shipped — the change is cheap. A
   semver value at or below the released app version means it is live, and step 1's hit is real.
3. **Does it have translations?** `list_lokalise_keys` with `filter_keys: "<key>"` and
   `include_translations: 1`. Any non-empty non-`en` translation is now **stale** the moment the
   source text changes, and it must be cleared and re-requested — that is the loc team's action,
   not the agent's (MCP cannot and should not rewrite translations of existing keys).

The sharp edge is **formatting**: adding or removing a placeholder changes the argument contract.
**It will not crash, which is worse.** `TextReference.Res` resolves through `getStringSafe`
(`core/res/.../Resources.kt`), which catches the format error, falls back to `getString(id)` with no
substitution and files a Crashlytics non-fatal — so a mismatch ships silently and users read the raw
template, e.g. `%1$s cashback in %2$s`. Don't reason about this as an exception you'd notice.
Gaining one where the call site passes nothing is a runtime crash; losing one leaves a now-unused
argument. Diff the placeholders (`%s`, `%d`, `%1$s`, …) before and after, in **every** language.

Report all three findings and get an explicit go-ahead. If the key is released or translated, prefer the
new-key route below over an in-place edit.

- **Breaking change** (meaning, number/type of arguments): do NOT touch the existing key. Create a new key (workflow above, including the confirmation gate; the replacement name is usually the old name + `_v2` suffix, precedents: `swapping_from_title_v2`, `express_provider_permission_needed_v2`), migrate the code, mark the old key:

```json
mcp__lokalise_sd__update_lokalise_key
{ "project_id": "4965953963bd330202ba50.61798973", "key_id": <id>, "tags": ["Deprecated"], "merge_tags": true }
```

  `merge_tags: true` is load-bearing: with `false` the call **replaces** the key's whole tag list,
  silently dropping the team tags the loc process relies on (`send_optional_field` carries `Send`,
  for instance). Never send `false` here. The tag is exactly `Deprecated` with a capital D (the loc
  team's actual process tag). Don't delete the old line from the XML — the loc team cleans it up after the release.
- **Text tweak without changing meaning/arguments**: still run both checks above. MCP can't update translations of existing keys — and shouldn't. Ask the user to edit the text in the Lokalise UI (app.lokalise.com, project App), have the loc team clear the affected translations, then pull with `lokalize.py`.

## Deleting strings

Forbidden. `Deprecated` tag only. Untoggling platforms and archiving are the loc team's decisions, not yours.

## Red flags — if you catch yourself doing this, go back to the workflow

- Edit/Write on `core/res/**/strings.xml` — it is a generated export; the only way it changes is a `lokalize.py` pull
- Dropping other teams' lines out of a pull to keep the diff "clean" (see step 7 — pull everything, commit everything)
- `%s`/`%d`/`%@` without square brackets in a text destined for Lokalise
- `curl api.lokalise.com` / raw REST with a token — MCP tools only
- Translating into languages other than `en`, or `create_lokalise_task` — translations are the loc team's process
- Renaming an existing key or changing its arguments
- Calling `create_lokalise_keys` with key names or tags the user never saw
- Changing the text of an existing key without first checking released status and translations

## Rationalizations vs reality

| Rationalization | Reality |
|---|---|
| "lokalize.py pulls the whole project's drift — easier to patch the XML by hand" | Hand edits diverge from Lokalise and get overwritten by the next pull. Run it unfiltered and commit what it produces |
| "Other teams' strings in my diff look like scope creep — I'll drop them" | The team pulls everything on purpose: it keeps the export current and prevents keys getting lost. Leave them in and say so in the PR |
| "Placeholders in strings.xml have no brackets — I'll do the same" | The repo XML is an export. Lokalise stores `[%d]`; verify with `list_lokalise_keys` + `include_translations: 1` |
| "MCP can't change translations — I'll hit the REST API" | Texts of existing keys are changed in the Lokalise UI, not by the agent |
| "I'll translate it myself and mark it unverified — linguists will review" | `en` only. The rest is the loc team's process |
| "An extra format argument won't break rendering" | Changing arguments is a breaking change: new key + `Deprecated` |
| "It's only a wording tweak, translations will catch up" | They won't — they silently stay stale in 10 languages. Check released status + translations first; clearing them is the loc team's action |
| "I'll create the key now, the translation can be added later" | Without an `en` translation the key isn't exported (`filter_data=translated`) — `R.string` won't appear and the build fails |
| "The names follow the convention, no need to ask" | The convention produces a **proposal**; the user decides the names and tags |