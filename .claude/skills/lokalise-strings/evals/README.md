# Evals for `lokalise-strings`

## How to run without touching Lokalise

The files themselves are inert JSON — authoring them changes nothing. **Running** them would:
cases 1 and 4 end in `create_lokalise_keys`, case 3 in an `update_lokalise_key` `Deprecated` tag,
and the skill hard-codes the live shared project (`App`, used by iOS and Android).

**Recommended mode — withhold the writes and grade the attempt.** Split the tools by direction:

| Direction | Tools | In a run |
|---|---|---|
| Write | `create_lokalise_keys`, `update_lokalise_key` | **withheld** — the attempt is the evidence |
| Read | `list_lokalise_keys`, `get_lokalise_key`, `python3 lokalize.py` | **allowed for real** — cases 2, 3 and 5 depend on them |

`lokalize.py` only downloads, so the pull is a read: leaving it live keeps the export faithful. Do
not tell the agent the write tools are missing — that would prompt the behaviour you are grading.
Let it attempt the call and read the intent off the transcript. A denied write is itself a probe: an
agent that responds by hand-editing `strings.xml` has broken a documented rule, and that is a
finding worth catching.

What this mode cannot cover: cases 1 and 4 never get a key created, so the pull cannot bring it in
and `R.string.<key>` never resolves — those two are graded up to the intended call, not to a
compiling build.

**Why not the `App Test` project** (`7815978369bbc8ae946df2.33345065`), even though it exists:

- It holds 2229 keys against the live project's 3122, so pointing `lokalize.py` at it rewrites
  `strings.xml` with a different key set — a huge bogus diff and a broken build.
- It accumulates state. Iteration 1 creates keys there, iteration 2 then finds them already
  present, and a **correct** run reuses them while the assertions still demand creation — exactly
  the inverted failure the pre-flight below exists to prevent.

Either way the repo needs isolating too: the pull rewrites `values*/strings.xml`, so give each run
a throwaway worktree rather than a shared checkout.

## Grading needs transcripts, not just outputs

Most assertions here are about *process* — searched by English text first, waited for approval,
ran the pull without `--langs`, piped the token out of the log. None of that is visible in an
output directory, so the grader must be given the run transcript. Assertions that can be checked
from files alone are the `strings.xml` ones.

## The repo is the fixture

No `files` are declared: the cases assume a checkout of this repository, which is what makes them
meaningful — case 2 needs `send_optional_field` to really be there, case 3 needs
`tangem_pay_transaction_details_cashback_below_min` to really be in `origin/releases/*`, and
cases 3 and 5 need `feature_toggles_config.json`. If the loc team retexts those keys, replace the
case rather than loosening the assertion.

## iteration-1 results (6 runs: cases 2, 3, 5 × with/without skill)

With skill **14/14**, baseline **6/14**; 304k vs 350k tokens — the skill ran ~15% *cheaper*, not
dearer. No run wrote to Lokalise: the withheld-writes design held.

What the skill demonstrably buys: both baselines proposed a **raw REST call with the API token in a
header**, both skipped the feature-toggle check, both offered to create a translation task, and the
case-3 baseline **edited two Kotlin files with no approval** while planning an in-place edit of a
released key shared with iOS.

What it does **not** buy: both baselines independently worked out from git history that
`values*/strings.xml` is generated and must not be hand-edited. That assertion — the rule the skill
states most emphatically — was dropped, along with three others that passed in both configurations.

Beyond the assertion set, both case-3 runs found that `ja`, `zh_CN` and `zh_TW` place `%2$s` first,
so dropping the placeholder crashes with `MissingFormatArgumentException` rather than merely reading
stale. That rationale moved into the case's `expected_output`.

## iteration-2 results (6 runs: cases 2, 3, 5 × with/without)

With skill 7/7, 6/6, 4/4; baseline 1/8, 1/6, ~3/4. The wider gap is **not** an improvement — it is
the sharper assertion set from iteration-1 doing its job. Baseline violations reproduced at n=2: raw
REST with the token in a header, no toggle check, self-created translation task, and in case 3 two
files edited with no approval plus an in-place edit of a released key.

**The most important result went against the skill.** Both with-skill runs claimed that dropping a
placeholder throws `MissingFormatArgumentException` in 10 locales. The 1/8 baseline proved otherwise:
`TextReference.Res` resolves through `getStringSafe`, which swallows the format error, falls back to
the un-substituted template and files a Crashlytics non-fatal. So the skill produced procedural
discipline but did not prevent a confident domain error — and this case's `expected_output` carried
the same mistake, since it was written from the iteration-1 with-skill reasoning. Both are fixed, and
the fact is now a gotcha in `SKILL.md`.

Curation applied: **case 2 removed** (4/4 vs ~3/4 across two iterations — a competent baseline
reaches `common_*` reuse unaided, so it measured the model), and the unfiltered-pull assertion
dropped (1 of 3 baselines chose it unaided, so it does not discriminate reliably).

## iteration-3 results (6 runs: cases 1, 3, 4 × with/without)

With skill 7/7, 6/7, 5/5. Baselines 3/7, 4/7, 5/5 — and note the baseline is **high-variance**
(1/8, 1/6, 3/7 across three runs of case 3): a single baseline run overstates the delta, which is
why the spec wants several runs per case.

**The hypothesis held.** After the `getStringSafe` gotcha landed, the case-3 with-skill run stopped
claiming a crash and traced the real behaviour itself. The `platforms`-for-replacement-keys amendment
also took effect, applied unprompted in two separate runs. That is the loop working: iteration-2
found the defects, the skill was corrected, iteration-3 shows the correction stuck.

**Case 4 discriminates not at all** — 5/5 both ways. Plurals are something a competent run gets right
unaided, because the repo carries a readable precedent (`tangempay_order_type_delivery_eta` plus its
`pluralReference` call site). The baseline even contributed a gotcha the skill lacked: the count must
be passed twice, as `count` *and* inside `formatArgs`. Now in `SKILL.md`. Case 4 is the next
candidate for removal.

**Two of my own methodology errors surfaced.** Dropping the unfiltered-pull assertion after
iteration-2 was premature — across four baselines three chose `--langs en`, so it discriminates ~75%
of the time; restored. And the evals leak their own answers into the run checkout (see the top of
this file).

Cases 1 and 4 both proved semantically impossible against the real product: no tracking field or
arrival date exists for case 1, and no remaining-attempts count exists for case 4, whose popup only
fires once the limit is already reached. They test the approval gate well but cannot test the
create-and-build path end to end.

## Status of the assertions

Authored from the skill's documented contract, **not** observed from a run — upstream guidance is
to add assertions after the first round of outputs. Treat them as a first draft: after
`iteration-1`, drop any that pass with *and* without the skill (they measure the model, not the
skill) and rewrite any that fail in both.

Five cases is above the "start with 2–3" guidance. Each maps to a distinct documented rule
(create · reuse-vs-new · breaking change · plurals · non-breaking tweak), but if the first
iteration is noisy, cut 4 and 5 first.

## Strip this directory from the run checkout — it leaks the answers

`evals.json` is committed *inside* the repository the runs search. It contains every prompt, the
`expected_output` and the assertions, so a run that greps for its own task can read the answer key.
This is not theoretical: an iteration-3 baseline reported hitting a line from this fixture while
grepping, and declined to open it. A less scrupulous run would have aced the case.

Delete it in each run's worktree before launching:

```bash
rm -rf <run-worktree>/.claude/skills/*/evals
```

Grading still works — the grader reads the fixture from *your* checkout, not the run's.

## Word the dry-run instruction to cover file writes, not just API writes

"Perform no mutating or write API calls" is not enough. An iteration-4 run reasonably read that as
permission to execute `python3 lokalize.py`, judging it safe because it exits before any network call
when unconfigured — which it did, only because `local.properties` was absent. In a fully configured
worktree that same run would have rewritten 11 files and ~2.5 MB of generated XML. Say instead:

> Perform no mutating API call and no command that writes to the repository — including the Lokalise
> pull. Print what you would run instead.

## Pre-flight: check the premises before every run

Each case assumes a specific state of `values/strings.xml`, and that state drifts as the loc team
and other features land keys. A case whose premise died doesn't fail loudly — it fails *inverted*:
a **correct** run reuses the key that already exists while the assertions demand creation. Run this
before trusting an iteration:

```bash
python3 - <<'EOF'
import io, re
x = io.open('core/res/src/main/res/values/strings.xml', encoding='utf-8').read()
t = lambda s: f'>{s}<' in x
k = lambda s: f'name="{s}"' in x
for name, ok in [
    # create-cases: the TARGET key must be absent, or a correct run reuses it and the
    # assertions fail inverted. This is the check that was missing and bit three times.
    ("case1 target absent",   not k('tangempay_delivery_status_track_button') and not t('Track delivery')),
    # fixtures the cases read
    ("case3 fixture: 2 args", '<string name="tangempay_cashback_widget_title">%1$s cashback in %2$s</string>' in x),
    ("case5 fixture: Fast options", '<string name="tangempay_choose_network_fast_way">Fast options</string>' in x),
    # change-cases: the requested NEW wording must not be what a release branch already holds,
    # or the case is a revert of in-flight loc work rather than the tweak it was written as.
    # Check by hand: git show origin/releases/<x.y>:<path> | grep '"<key>"'
]:
    print(('PASS  ' if ok else 'FAIL  ') + name)
EOF
```

This is not hypothetical: three of the five cases were authored against the task that produced this
skill's own keys, so their premises were already satisfied by the repo — cases 1, 3 and 4 all had to
be repointed at absent or unchanged targets before they tested anything.

## Known limitations of this set

- **Cases 3 and 5 target keys other teams actively retext.** Case 3 already had to be rewritten
  once: it pointed at `tangem_pay_transaction_details_cashback_below_min`, which the loc team
  changed to exactly the text the prompt asked for, turning the case into a no-op. Re-verify both
  against the current export before trusting a run.
- **The ` · ` in query 10 and in case 2 is deliberate bait.** People phrase the request as
  « · Optional», but the separator is Compose layout and must not land in the key. `eval_queries.json`
  only measures activation, so the dot is inert there; case 2 is what actually grades it.
- **Query 10 in `eval_queries.json` is deliberately multi-skill** ("…прогони detekt и открой PR").
  It should trigger this skill, but it will legitimately trigger others too — do not treat a
  co-trigger there as a false positive.

## iteration-4 results (4 runs: cases 1, 6 × with/without)

Case 1: 9/9 vs 6/9. Case 6 (new, the `_android` platform split): 6/7 vs 5/7.

Case 1's discrimination is **purely mechanical** — unfiltered pull and piping the token out of the
log. The two assertions added to make the case honest (no tracking field exists; the request
contradicts the recorded email-only decision) passed in *both* configurations, so they were unscored
again. Both runs also established the `local.properties` blocker empirically this time, by running
the script and reading its refusal.

Case 6 works as designed but thinly. The baseline derived the whole architecture unaided — split the
key, `platforms: ["android"]`, narrow the original to `["ios"]` — because that follows from the
constraint. It failed only on what cannot be derived: it invented a `_v2` suffix with no
`android-specific` tag, conflating the replacement convention with the platform-split one, and it
created a translation task itself. Both gaps are now explicit in `SKILL.md`.

**Conclusion after four iterations: the skill's value concentrates in one place.** Case 3 — changing
an existing released, translated key — is the only strong discriminator (7/7 vs 1/8, 3/7). Every
other case converges, because a competent run reads the repo's own precedents. The dangerous-change
gate is what the skill buys; the create path and the naming conventions are mostly confirmation.

## The set as it stands

Three cases, matching the upstream "start with 2–3" guidance — not by design but by attrition:

| Case | What it tests | iteration-3 |
|---|---|---|
| 1 | create path: text-first search, approval gate, en-only, unfiltered pull, token hygiene | 6/7 vs 4/7 |
| 3 | breaking change: three-check gate, new-key route, `Deprecated`, no raw REST, no self-made task | 7/7 vs 3/7 |
| 5 | non-breaking tweak on a released, translated key: route to loc rather than act | 6/6 vs 1/6 |

Case 2 and case 4 were removed after scoring ~even in both configurations across two iterations
apiece. Case 3 is the only strong discriminator; cases 1 and 5 discriminate mostly on mechanics
(unfiltered pull, piping the token out of the log). Case 1 is also known to be unbuildable on
purpose — the point is that the agent says so instead of inventing backend fields.

## Not covered yet

The `_android`-suffix divergence, the `strings_special.xml` boundary (covered as a negative in
`eval_queries.json` instead), a request to **delete** a key (which the skill forbids outright — no
case proves the agent refuses), plurals (case 4's subject, dropped because it did not discriminate),
and a full `Deprecated` audit sweep.