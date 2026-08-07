---
name: navigation-graph
description: Refresh the app's navigation/dependency graph from live code and rebuild the interactive visualization. Scans features/domain Gradle project dependencies and every AppRoute usage, updates the generated regions of .claude/docs/navigation-graph.md (preserving hand-written prose and the curated config), then renders .claude/docs/module-connectivity.html (Area / Module / Screens views with team overlays). Use when the user asks to update/regenerate/refresh the navigation graph, screen map, module connectivity diagram or module-connectivity.html, after adding/removing AppRoute screens or feature/domain modules, or to add/recolor a team. Triggers: "update the navigation graph", "regenerate module-connectivity.html", "refresh the screen map", "rebuild the module connectivity diagram", "add a team to the graph".
allowed-tools: Bash, Read, Edit, Grep, Glob
---

Refresh and rebuild the app's connectivity visualization. The data flows **code → doc → HTML**:

```
features/domain/data build.gradle.kts deps ─┐
AppRoute.kt + every AppRoute.X usage        ─┴─▶ build_graph.py ─▶ .claude/docs/navigation-graph.md
                                                                      │ (data + curated config blocks)
                                                                      ▼
                                                          render_html.py ─▶ .claude/docs/module-connectivity.html
```

`navigation-graph.md` is the source of truth. Its hand-written prose (sections 1–4: route tables, edges-with-triggers, nested routes, deep links) is **owned by humans and never overwritten**. The skill manages only the region between `<!-- NAVGRAPH:BEGIN -->` and `<!-- NAVGRAPH:END -->`, which holds:
- an **editable CONFIG json block** — functional groups (label/color/grid anchor), per-screen group + owner, and teams. Preserved across refreshes (human edits win).
- **auto data blocks** — `areaGraph`, `moduleGraph`, `screensGraph`, `screensMeta`. Overwritten from code every run.

## To refresh after a code change (the common case)

Run both scripts from anywhere in the repo (they locate the root via `settings.gradle.kts`). The scan walks the whole tree — expect ~10–20s.

```bash
python3 .claude/skills/navigation-graph/scripts/build_graph.py
python3 .claude/skills/navigation-graph/scripts/render_html.py
```

Then report to the user: the printed counts, **any `⚠ NEW screen` warnings**, and that `.claude/docs/module-connectivity.html` is a self-contained file they can open in a browser.

**If `build_graph.py` prints `⚠ NEW screen …` warnings:** a route was added to `AppRoute.kt` but isn't classified. The new screen was defaulted to group `misc` / owner `app`. Edit the CONFIG block in `.claude/docs/navigation-graph.md` to set its real `screenGroups[Name]` and `screenOwners[Name]`, then re-run both scripts. Pick the group/owner by reading where the route lives and is pushed from. Removed routes are reported as "no longer in code" and are harmless (kept in config for when they return).

## To change grouping, owners, colors, or teams

Edit the **CONFIG** json block inside `.claude/docs/navigation-graph.md` (between `<!-- NAVGRAPH:config:BEGIN -->` and `:END`), then run `render_html.py` (no need to re-scan code unless code changed):
- **`groups`** — add/rename a functional group or change its `color` / `label` / grid `anchor` (`x`,`y` are 0–1 fractions of the canvas).
- **`screenGroups` / `screenOwners`** — reassign a screen.
- **`teams`** — add a team object `{ "id", "name", "color", "roots": [...module path prefixes...], "screens": [...AppRoute names...] }`. `roots` drive the overlay in Area/Module views (e.g. `"features:onramp"`, `"domain:staking"`, `"data:swap"`); `screens` drive it in the Screens view (e.g. `"Send"`). A module/screen may be matched by either. The overlay (hull + rings, optional cluster force) and per-view member counts wire up automatically.

After editing CONFIG, re-run `render_html.py`. If you changed code-derived facts, run `build_graph.py` first.

## What gets extracted (and what's inferred)

- **Dependency edges** (Area/Module): only project (`projects.*`) deps among `features/*`, `domain/*` and `data/*` — external libraries excluded. Area view merges each area's `api`/`impl`/`models`. The Data-layer toggle in the HTML appears only when data modules are present.
- **Screen edges** (Screens): the **target** is exact — the `AppRoute.X` argument of a `push`/`replaceCurrent`/`replaceAll`/`popTo` call. The **source** is the navigating file's feature module collapsed to that feature's main screen (eponymous screen if one exists, else most-referenced), so intra-feature hops are merged. Calls from shared UI / app root attach to a synthetic **App shell** node. Group/owner come from the curated CONFIG.

## Files

```
.claude/skills/navigation-graph/
  SKILL.md
  assets/template.html        # parameterized HTML (8 inject points: 4 data blocks + teams + group colors/labels/anchors)
  assets/config.seed.json     # initial curation; used only when the doc has no CONFIG block yet
  scripts/build_graph.py      # code  -> navigation-graph.md (managed region)
  scripts/render_html.py      # navigation-graph.md -> module-connectivity.html
```

Outputs live in `.claude/docs/`. The scripts are idempotent — re-running never duplicates the managed region. Do not hand-edit the auto data blocks (they're regenerated); edit CONFIG instead. After regenerating, sanity-check the HTML by extracting its `<script>` and running `node --check` if Node is available.