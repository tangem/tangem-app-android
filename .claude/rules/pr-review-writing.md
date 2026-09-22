# Writing PRs & Review Comments

Everything a human reads — PR title and description, review comments, replies to reviewers, Jira
comments — is **short**. The reader is an engineer with the diff already open, not someone who needs
the change re-explained. Say the point once, in as few words as it takes.

## Limits

| What | Limit |
|---|---|
| PR description (`## What`; footer and over-limit justification excluded) | **≤ 500 characters**, 1–3 sentences |
| One review comment | **≤ 350 characters**, 1–2 sentences |
| Reply to a review comment | one sentence — `Fixed in <sha>`, `Intentional: <reason>` |
| Review summary / verdict comment | **don't post one** — comment where the problem is, or say "ok" in one line |

These are caps, not targets. Over the cap → cut words, never split into a second comment.

## What to post in a review

Post **only findings you would block the merge on** — a bug, a broken contract, a real regression
risk. Everything else (style, naming, preferences, "could be nicer") the author reads with their own
eyes: leave it out, or one line prefixed `nit:`.

More than ~5 comments on a PR means talk to the author instead of posting a wall.

## Never write

- A retelling of the diff (`This PR adds FooRepository, which is injected into BarModel and…`) — the
  reviewer reads the diff, not the description
- Boilerplate sections (`## Changes`, `## Impact`, `## Testing`, `## Risk assessment`), a bullet per
  touched file, summary tables — unless they carry information the diff does not
- Report formatting in a review comment (`**Severity: Medium**`, `**Impact:**`,
  `**Recommendation:**`, emoji verdict headers) — state the problem instead
- Praise and filler (`Great work overall!`, `Nice use of coroutines here`)
- The same point twice — in the summary and again inline

## Example

```
❌ **Severity: Medium — Potential race condition**
   `refresh()` is invoked from `modelScope`, which means that when the component is destroyed…
   (two more paragraphs, then a "Recommendation" block)

✅ `refresh()` can outlive `onDestroy()` — the mutex guards the map, not the scope. Cancel before
   the launch.
```

## Not covered by this rule

The fixed report templates in the `fix-crashlytics` skill (Jira `**Claude Report**` comment, the
`**Root cause:** / **Fix:** / **Reach:**` PR body). There the labelled fields are the content — one
line per slot, machine-generated, read as a record. Keep those as they are.