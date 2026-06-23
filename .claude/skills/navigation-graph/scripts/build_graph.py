#!/usr/bin/env python3
"""
build_graph.py — scan the live codebase and refresh the managed regions of
.claude/docs/navigation-graph.md (the source of truth for module-connectivity.html).

What it extracts from code:
  - features/domain/data Gradle project dependencies -> area graph + module graph
  - AppRoute screens + every `AppRoute.X` reference -> screen navigation graph

What it preserves:
  - all hand-written prose ABOVE the <!-- NAVGRAPH:BEGIN --> marker
  - the curated CONFIG block (groups / owners / teams) inside the managed region

Run from anywhere inside the repo:  python3 build_graph.py
Then render the HTML:               python3 render_html.py
"""
import os, re, json, sys
from collections import defaultdict

# ---------------------------------------------------------------- paths
def find_root(start):
    d = os.path.abspath(start)
    while d != os.path.dirname(d):
        if os.path.exists(os.path.join(d, "settings.gradle.kts")):
            return d
        d = os.path.dirname(d)
    sys.exit("ERROR: could not locate repo root (settings.gradle.kts not found).")

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = find_root(os.getcwd())
SKILL = os.path.dirname(SCRIPT_DIR)
DOCS = os.path.join(ROOT, ".claude", "docs")
MD = os.path.join(DOCS, "navigation-graph.md")
SEED = os.path.join(SKILL, "assets", "config.seed.json")
APPROUTE = os.path.join(ROOT, "common/routing/src/main/kotlin/com/tangem/common/routing/AppRoute.kt")

# ---------------------------------------------------------------- managed-region helpers
BEGIN = "<!-- NAVGRAPH:BEGIN — managed by the navigation-graph skill. Edit only the CONFIG json; the rest is generated from code. -->"
END = "<!-- NAVGRAPH:END -->"

def block(text, key):
    """Return the JSON string inside the <!-- NAVGRAPH:key:BEGIN/END --> markers, or None."""
    m = re.search(r"<!-- NAVGRAPH:%s:BEGIN -->\s*```json\s*(.*?)\s*```\s*<!-- NAVGRAPH:%s:END -->"
                  % (re.escape(key), re.escape(key)), text, re.S)
    return m.group(1) if m else None

def wrap(key, payload):
    return f"<!-- NAVGRAPH:{key}:BEGIN -->\n```json\n{payload}\n```\n<!-- NAVGRAPH:{key}:END -->"

# ---------------------------------------------------------------- gradle dependency scan
def camel_to_kebab(s): return re.sub(r'(?<!^)(?=[A-Z])', '-', s).lower()
def accessor_to_path(acc): return ':' + ':'.join(camel_to_kebab(p) for p in acc.split('.'))
def dir_to_path(d): return ':' + os.path.relpath(d, ROOT).replace(os.sep, ':')

PROJ_RE = re.compile(r'(api|implementation|testImplementation|androidTestImplementation|'
                     r'compileOnly|kapt|ksp|debugImplementation)\s*\(\s*projects\.([A-Za-z0-9_.]+)\s*\)')

def scan_modules():
    modules = {}
    edges = []
    for dp, _, fns in os.walk(ROOT):
        if '/build/' in dp or '/.git' in dp or '/.gradle' in dp: continue
        if 'build.gradle.kts' not in fns: continue
        if dp == ROOT: continue
        modules[dir_to_path(dp)] = dp
    for path, dp in modules.items():
        txt = open(os.path.join(dp, 'build.gradle.kts'), encoding='utf-8', errors='ignore').read()
        for m in PROJ_RE.finditer(txt):
            edges.append((path, accessor_to_path(m.group(2)), m.group(1)))
    return modules, edges

SCOPED_LAYERS = ('features', 'domain', 'data')
def is_scoped(m): return any(m.startswith(':' + layer) for layer in SCOPED_LAYERS)
def area_of(m):
    p = m.split(':')[1:]
    return f"{p[0]}:{p[1]}" if len(p) > 1 and p[0] in SCOPED_LAYERS else None

def build_area_graph(modules, edges):
    agg = defaultdict(lambda: {"w": 0, "api": False})
    for s, d, c in edges:
        if not (is_scoped(s) and is_scoped(d)): continue
        sa, da = area_of(s), area_of(d)
        if not sa or not da or sa == da: continue
        agg[(sa, da)]["w"] += 1
        if c == 'api': agg[(sa, da)]["api"] = True
    indeg, outdeg = defaultdict(int), defaultdict(int)
    for (s, d) in agg: outdeg[s] += 1; indeg[d] += 1
    nodes = sorted({n for e in agg for n in e})
    N = [[n, n.split(':')[1], n.split(':')[0], indeg[n], outdeg[n]] for n in nodes]
    E = [[s, d, v["w"], 1 if v["api"] else 0] for (s, d), v in agg.items()]
    return {"n": N, "e": E}

def build_module_graph(modules, edges):
    agg = defaultdict(lambda: {"w": 0, "api": False})
    for s, d, c in edges:
        if not (is_scoped(s) and is_scoped(d)) or s == d: continue
        agg[(s, d)]["w"] += 1
        if c == 'api': agg[(s, d)]["api"] = True
    indeg, outdeg = defaultdict(int), defaultdict(int)
    for (s, d) in agg: outdeg[s] += 1; indeg[d] += 1
    conn = {n for e in agg for n in e}
    N = [[m, ':'.join(m.split(':')[2:]), m.split(':')[1], indeg[m], outdeg[m]]
         for m in sorted(conn)]
    E = [[s, d, v["w"], 1 if v["api"] else 0] for (s, d), v in agg.items()]
    return {"n": N, "e": E}

# ---------------------------------------------------------------- AppRoute scan
REF_RE = re.compile(r'AppRoute\.([A-Z][A-Za-z0-9]+)')
DECL_RE = re.compile(r'^    (?:data )?(?:object|class) (\w+)', re.M)

def read_kotlin_string(text, i):
    """text[i] must be '"'. Return (content, index_after_closing_quote), handling \\"
    escapes and ${...} template expressions (nested braces/strings copied verbatim) so a
    path with string templates isn't truncated at the first inner quote."""
    out, j = [], i + 1
    while j < len(text):
        c = text[j]
        if c == '\\':
            out.append(text[j:j + 2]); j += 2; continue
        if c == '"':
            return ''.join(out), j + 1
        if c == '$' and j + 1 < len(text) and text[j + 1] == '{':
            out.append('${'); j += 2; depth = 1
            while j < len(text) and depth > 0:
                ck = text[j]
                if ck == '"':
                    s, j = read_kotlin_string(text, j); out.append('"' + s + '"'); continue
                if ck == '{': depth += 1
                elif ck == '}': depth -= 1
                if depth > 0: out.append(ck)
                j += 1
            out.append('}'); continue
        out.append(c); j += 1
    return ''.join(out), j

def extract_path(block):
    """First string literal of the `path = …` argument within one screen's source block.
    Block-scoped (so multi-line `AppRoute(` blocks resolve) and template-aware (so paths
    aren't truncated); for a non-literal RHS (e.g. `path = when {…}`) it takes the first
    branch literal. Returns None when the block has no `path =`."""
    m = re.search(r'\bpath\s*=\s*', block)
    if not m: return None
    i = m.end()
    while i < len(block) and block[i] in ' \t\r\n': i += 1
    if i < len(block) and block[i] == '"':
        return read_kotlin_string(block, i)[0]
    q = block.find('"', i)
    return read_kotlin_string(block, q)[0] if q != -1 else None

def scan_routes():
    src = open(APPROUTE, encoding='utf-8').read()
    decls = [(m.group(1), m.start()) for m in DECL_RE.finditer(src)]
    screens = {name for name, _ in decls}
    paths = {}
    for idx, (name, start) in enumerate(decls):
        end = decls[idx + 1][1] if idx + 1 < len(decls) else len(src)
        p = extract_path(src[start:end])
        if p is not None: paths.setdefault(name, p)
    usage = defaultdict(lambda: defaultdict(int))
    nav = defaultdict(int)
    def area(fp):
        parts = os.path.relpath(fp, ROOT).split(os.sep)
        top = parts[0]
        return f"{top}:{parts[1]}" if top in ('features','domain','data','core','common','libs') and len(parts) > 1 else top
    for dp, _, fns in os.walk(ROOT):
        if '/build/' in dp or '/.git' in dp or '/.gradle' in dp: continue
        for fn in fns:
            if not fn.endswith('.kt'): continue
            fp = os.path.join(dp, fn)
            if os.path.samefile(fp, APPROUTE) if os.path.exists(APPROUTE) else False: continue
            try: txt = open(fp, encoding='utf-8', errors='ignore').read()
            except Exception: continue
            if 'AppRoute.' not in txt: continue
            a = area(fp)
            for m in REF_RE.finditer(txt):
                if m.group(1) in screens: usage[m.group(1)][a] += 1
            for nm in re.finditer(r'\b(push|replaceCurrent|replaceAll|popTo)\s*\(', txt):
                r2 = REF_RE.search(txt[nm.end():nm.end()+160])
                if r2 and r2.group(1) in screens: nav[(a, r2.group(1))] += 1
    return screens, paths, usage, nav

def build_screen_graph(screens, paths, usage, nav, cfg):
    total = {s: sum(usage[s].values()) for s in screens}
    sg, so = cfg["screenGroups"], cfg["screenOwners"]
    owned = defaultdict(list)
    for s in sorted(screens): owned[so.get(s, cfg["defaultOwner"])].append(s)
    def fnorm(a): return a.split(':')[-1].replace('-', '').lower()
    def main_of(a, ss):
        epon = [s for s in ss if s.lower() == fnorm(a)]
        return epon[0] if epon else max(ss, key=lambda x: (total[x], x))  # deterministic tie-break
    main = {a: main_of(a, ss) for a, ss in owned.items()}
    APPSHELL = 'AppShell'
    edge = defaultdict(int)
    for (a, t), c in nav.items():
        src = main.get(a, APPSHELL)
        if src == t: continue
        edge[(src, t)] += c
    use_shell = any(s == APPSHELL for s, _ in edge)
    indeg, outdeg = defaultdict(int), defaultdict(int)
    for (s, t) in edge: outdeg[s] += 1; indeg[t] += 1
    N = [[s, s, sg.get(s, cfg["defaultGroup"]), indeg[s], outdeg[s]] for s in sorted(screens)]
    if use_shell: N.append([APPSHELL, 'App shell', 'shell', indeg[APPSHELL], outdeg[APPSHELL]])
    E = [[s, t, w, 0] for (s, t), w in edge.items()]
    meta = {}
    for s in sorted(screens):
        meta[s] = {"path": paths.get(s, ""), "owner": so.get(s, cfg["defaultOwner"]),
                   "group": sg.get(s, cfg["defaultGroup"]), "total": total[s],
                   "refs": sorted(usage[s].items(), key=lambda kv: -kv[1])}
    return {"n": N, "e": E}, meta

# ---------------------------------------------------------------- main
def main():
    if not os.path.exists(APPROUTE):
        sys.exit(f"ERROR: AppRoute.kt not found at {APPROUTE}")
    old = open(MD, encoding='utf-8').read() if os.path.exists(MD) else ""

    # config: prefer the one already in the doc (human edits win), else seed
    cfg_str = block(old, "config")
    cfg = json.loads(cfg_str) if cfg_str else json.load(open(SEED))

    modules, dep_edges = scan_modules()
    area_g = build_area_graph(modules, dep_edges)
    mod_g = build_module_graph(modules, dep_edges)
    screens, paths, usage, nav = scan_routes()

    # reconcile config with the screens actually present in code
    warns = []
    for s in sorted(screens):
        if s not in cfg["screenGroups"]:
            cfg["screenGroups"][s] = cfg["defaultGroup"]; warns.append(f"NEW screen '{s}': group defaulted to '{cfg['defaultGroup']}' — set it in CONFIG")
        if s not in cfg["screenOwners"]:
            cfg["screenOwners"][s] = cfg["defaultOwner"]; warns.append(f"NEW screen '{s}': owner defaulted to '{cfg['defaultOwner']}' — set it in CONFIG")
    stale = [s for s in cfg["screenGroups"] if s not in screens]

    screen_g, screen_meta = build_screen_graph(screens, paths, usage, nav, cfg)

    inv_area = sum(1 for s, t, w, a in area_g["e"] if t.startswith('features:') and not s.startswith('features:'))

    cj = lambda o: json.dumps(o, separators=(',', ':'))
    summary = (
        f"_Auto-generated from code by the `navigation-graph` skill. Edit only the CONFIG block below._\n\n"
        f"- **Screens (AppRoute):** {len(screens)} · screen-nav edges {len(screen_g['e'])}\n"
        f"- **Area graph:** {len(area_g['n'])} feature/domain/data areas · {len(area_g['e'])} dependency edges · {inv_area} inverted (domain/data→features)\n"
        f"- **Module graph:** {len(mod_g['n'])} modules · {len(mod_g['e'])} edges\n"
    )
    if warns: summary += "\n**Action needed:**\n" + "\n".join(f"- {w}" for w in warns) + "\n"
    if stale: summary += f"\n_Config has {len(stale)} screen(s) no longer in code (kept, harmless): {', '.join(stale)}_\n"

    managed = "\n\n".join([
        BEGIN,
        "## Connectivity data (generated)\n\n" + summary,
        "### Curated config (editable — preserved across refreshes)\n\n" + wrap("config", json.dumps(cfg, indent=2)),
        "### Graph data (auto — overwritten every refresh; do not hand-edit)\n\n" +
        wrap("areaGraph", cj(area_g)) + "\n\n" + wrap("moduleGraph", cj(mod_g)) + "\n\n" +
        wrap("screensGraph", cj(screen_g)) + "\n\n" + wrap("screensMeta", cj(screen_meta)),
        END,
    ])

    if BEGIN in old and END in old:
        head = old[:old.index(BEGIN)].rstrip() + "\n\n"
        tail = old[old.index(END) + len(END):]
        new = head + managed + tail
    elif old.strip():
        new = old.rstrip() + "\n\n" + managed + "\n"
    else:
        new = "# Navigation Graph\n\n" + managed + "\n"

    os.makedirs(DOCS, exist_ok=True)
    open(MD, "w", encoding='utf-8').write(new)
    print(f"✓ updated {os.path.relpath(MD, ROOT)}")
    print(f"  screens={len(screens)} screen-edges={len(screen_g['e'])} | "
          f"areas={len(area_g['n'])}/{len(area_g['e'])} | modules={len(mod_g['n'])}/{len(mod_g['e'])}")
    for w in warns: print("  ⚠ " + w)
    print("Next: python3 " + os.path.join(SCRIPT_DIR, "render_html.py"))

if __name__ == "__main__":
    main()