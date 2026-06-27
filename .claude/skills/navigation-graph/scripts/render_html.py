#!/usr/bin/env python3
"""
render_html.py — build .claude/docs/module-connectivity.html from the data blocks
in .claude/docs/navigation-graph.md (which build_graph.py refreshes from code).

Run AFTER build_graph.py:  python3 render_html.py
"""
import os, re, json, sys

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
MD = os.path.join(ROOT, ".claude", "docs", "navigation-graph.md")
TEMPLATE = os.path.join(SKILL, "assets", "template.html")
OUT = os.path.join(ROOT, ".claude", "docs", "module-connectivity.html")

def block(text, key):
    m = re.search(r"<!-- NAVGRAPH:%s:BEGIN -->\s*```json\s*(.*?)\s*```\s*<!-- NAVGRAPH:%s:END -->"
                  % (re.escape(key), re.escape(key)), text, re.S)
    if not m:
        sys.exit(f"ERROR: data block '{key}' not found in {MD}. Run build_graph.py first.")
    return json.loads(m.group(1))

def main():
    if not os.path.exists(MD): sys.exit(f"ERROR: {MD} not found. Run build_graph.py first.")
    if not os.path.exists(TEMPLATE): sys.exit(f"ERROR: template missing at {TEMPLATE}")
    text = open(MD, encoding='utf-8').read()
    cfg = block(text, "config")
    area = block(text, "areaGraph")
    mod = block(text, "moduleGraph")
    screens = block(text, "screensGraph")
    smeta = block(text, "screensMeta")

    groups = cfg["groups"]
    group_colors = {g: groups[g]["color"] for g in groups}
    group_labels = {g: groups[g]["label"] for g in groups}
    group_anchors = {g: groups[g]["anchor"] for g in groups}

    cj = lambda o: json.dumps(o, separators=(',', ':'))
    tpl = open(TEMPLATE, encoding='utf-8').read()
    repl = {
        "/*__AREA_DATA__*/null": cj(area),
        "/*__MODULE_DATA__*/null": cj(mod),
        "/*__SCREENS_DATA__*/null": cj(screens),
        "/*__SCREENS_META__*/null": cj(smeta),
        "/*__TEAMS__*/[]": cj(cfg["teams"]),
        "/*__GROUP_COLORS__*/{}": cj(group_colors),
        "/*__GROUP_LABELS__*/{}": cj(group_labels),
        "/*__GROUP_ANCHORS__*/{}": cj(group_anchors),
    }
    out = tpl
    for k, v in repl.items():
        if k not in out: sys.exit(f"ERROR: placeholder '{k}' missing in template — template/skill version mismatch.")
        out = out.replace(k, v)
    for ph in ("__AREA_DATA__", "__MODULE_DATA__", "__SCREENS_DATA__", "__SCREENS_META__",
               "__TEAMS__", "__GROUP_COLORS__", "__GROUP_LABELS__", "__GROUP_ANCHORS__"):
        if "/*" + ph + "*/" in out: sys.exit(f"ERROR: placeholder {ph} left unreplaced.")
    open(OUT, "w", encoding='utf-8').write(out)
    print(f"✓ wrote {os.path.relpath(OUT, ROOT)}  ({len(out)//1024} KB, self-contained)")
    print(f"  area {len(area['n'])}n/{len(area['e'])}e · module {len(mod['n'])}n/{len(mod['e'])}e · "
          f"screens {len(screens['n'])}n/{len(screens['e'])}e · teams {len(cfg['teams'])}")
    print(f"  open: {OUT}")

if __name__ == "__main__":
    main()