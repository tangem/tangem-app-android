---
name: lokalise-strings
description: Use when adding, changing, or deprecating user-facing localized strings in this app — new UI text, string resources, Lokalise keys, R.string / R.plurals, TextReference, strings.xml under core/res, plurals, placeholders, missing translations. Триггеры: «добавь/заведи строку», «завести ключ в локалайзе», «поменяй текст», «нужна строка для экрана».
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
| `platforms` | always `["ios", "android"]` |
| Key names & tags | agent proposes — **the user approves**; never create unconfirmed |
| Translations at creation | **`en` only** |
| Pull into the repo | `python3 lokalise.py --langs en` (needs `lokalise.project.id` and `lokalise.token` in `local.properties`; `pip3 install python-lokalise-api --break-system-packages`) |

## Workflow: adding a string

1. **Check for an existing key**: grep `core/res/src/main/res/values/strings.xml`; when in doubt — `list_lokalise_keys`.
2. **Draft key names**: `<feature>_<screen>_<element>`, snake_case. Prefix — same as the neighboring key cluster of that screen (grep; e.g. `tangempay_current_plan_*`). Never rename existing keys.
3. **Draft the EN text in universal-placeholder format** (table below). Take the format from that table, NOT from the repo XML — the XML contains the already-exported Android format.
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
6. **Pull into the repo**: `python3 lokalise.py --langs en`. Without this step `R.string.<key>` doesn't exist and the build fails.
7. **`git diff core/res`**: commit only the lines of your keys. Surgically exclude other teams' drift from the commit (`git add -p` / restore specific lines to HEAD). Never revert with broad commands like `git checkout -- .` — the drift will arrive with the regular "[Tech] lokalize" commit.
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

If the Android form must differ from iOS — a separate key with the `_android` suffix and `platforms: ["android"]` (examples: `common_days`, `manage_tokens_number_of_wallets_android`).

## Changing an existing string

- **Breaking change** (meaning, number/type of arguments): do NOT touch the existing key. Create a new key (workflow above, including the confirmation gate; the replacement name is usually the old name + `_v2` suffix, precedents: `swapping_from_title_v2`, `express_provider_permission_needed_v2`), migrate the code, mark the old key:

```json
mcp__lokalise_sd__update_lokalise_key
{ "project_id": "4965953963bd330202ba50.61798973", "key_id": <id>, "tags": ["Deprecated"], "merge_tags": true }
```

  The tag is exactly `Deprecated` with a capital D (the loc team's actual process tag). Don't delete the old line from the XML — the loc team cleans it up after the release.
- **Text tweak without changing meaning/arguments**: MCP can't update translations of existing keys — and shouldn't. Ask the user to edit the text in the Lokalise UI (app.lokalise.com, project App), then pull with `lokalize.py`.

## Deleting strings

Forbidden. `Deprecated` tag only. Untoggling platforms and archiving are the loc team's decisions, not yours.

## Red flags — if you catch yourself doing this, go back to the workflow

- Edit/Write on `core/res/**/strings.xml` (except excluding drift hunks from a commit in step 7)
- `%s`/`%d`/`%@` without square brackets in a text destined for Lokalise
- `curl api.lokalise.com` / raw REST with a token — MCP tools only
- Translating into languages other than `en`, or `create_lokalise_task` — translations are the loc team's process
- Renaming an existing key or changing its arguments
- Calling `create_lokalise_keys` with key names or tags the user never saw

## Rationalizations vs reality

| Rationalization | Reality |
|---|---|
| "lokalize.py pulls the whole project's drift — easier to patch the XML by hand" | Hand edits diverge from Lokalise and get overwritten by the next pull. Run `--langs en` and don't commit others' hunks |
| "Placeholders in strings.xml have no brackets — I'll do the same" | The repo XML is an export. Lokalise stores `[%d]`; verify with `list_lokalise_keys` + `include_translations: 1` |
| "MCP can't change translations — I'll hit the REST API" | Texts of existing keys are changed in the Lokalise UI, not by the agent |
| "I'll translate it myself and mark it unverified — linguists will review" | `en` only. The rest is the loc team's process |
| "An extra format argument won't break rendering" | Changing arguments is a breaking change: new key + `Deprecated` |
| "I'll create the key now, the translation can be added later" | Without an `en` translation the key isn't exported (`filter_data=translated`) — `R.string` won't appear and the build fails |
| "The names follow the convention, no need to ask" | The convention produces a **proposal**; the user decides the names and tags |