#!/usr/bin/env python3
"""Sets feature toggle versions across all stream feature_toggles.json files.

Usage: set_toggles.py [--check [--ref <git-ref>]] <X.Y> NAME [NAME=version ...]
  NAME          -> version X.Y (enabled in this release)
  NAME=version  -> exactly that version (X.Y[.Z] or undefined), applied as is

With no names it does nothing. Fails without writing anything if a name is not found in any stream file.
--check only validates the names and prints the planned changes. With --ref it reads the files from
that git ref (e.g. origin/develop) instead of the working tree. Run from the repo root.
"""
import fnmatch
import glob
import json
import re
import subprocess
import sys

TOGGLES_GLOB = "*/config-toggles/feature_toggles.json"
DISABLED_VERSION = "undefined"
VERSION_PATTERN = re.compile(r"^[0-9]+\.[0-9]+(\.[0-9]+)?$")


def load_configs(ref):
    if ref is None:
        return {path: json.load(open(path, encoding="utf-8")) for path in sorted(glob.glob(TOGGLES_GLOB))}
    listed = subprocess.run(
        ["git", "ls-tree", "-r", "--name-only", ref], check=True, capture_output=True, text=True
    ).stdout.splitlines()
    paths = sorted(p for p in listed if fnmatch.fnmatch(p, TOGGLES_GLOB) and p.count("/") == 2)
    return {
        path: json.loads(subprocess.run(["git", "show", f"{ref}:{path}"], check=True, capture_output=True, text=True).stdout)
        for path in paths
    }


def parse_version(value: str):
    parts = value.split(".")
    return tuple(int(p) for p in parts) if all(p.isdigit() for p in parts) else None


def is_earlier(current: str, release: str) -> bool:
    current_parsed, release_parsed = parse_version(current), parse_version(release)
    return current_parsed is not None and release_parsed is not None and current_parsed < release_parsed


def main() -> int:
    args = sys.argv[1:]
    check_only = "--check" in args
    args = [a for a in args if a != "--check"]
    ref = None
    if "--ref" in args:
        index = args.index("--ref")
        ref = args[index + 1]
        del args[index:index + 2]
        if not check_only:
            print("--ref is only allowed with --check", file=sys.stderr)
            return 2
    if not args:
        print(__doc__, file=sys.stderr)
        return 2

    release_version = args[0]
    requested = {}
    explicit = set()
    invalid = []
    for arg in args[1:]:
        name, separator, version = arg.partition("=")
        name, version = name.strip(), version.strip()
        if separator:
            if version != DISABLED_VERSION and not VERSION_PATTERN.match(version):
                invalid.append(arg)
            explicit.add(name)
        else:
            explicit.discard(name)
        requested[name] = version or release_version

    if invalid:
        print(f"Invalid versions (expected X.Y[.Z] or {DISABLED_VERSION}): " + ", ".join(invalid), file=sys.stderr)
        return 1

    configs = load_configs(ref)

    found = {}
    for path, toggles in configs.items():
        for toggle in toggles:
            if toggle["name"] in requested:
                found[toggle["name"]] = (path, toggle)

    missing = sorted(set(requested) - set(found))
    if missing:
        print("Unknown toggles (not in any stream file): " + ", ".join(missing), file=sys.stderr)
        return 1

    changed_files = set()
    for name, version in requested.items():
        path, toggle = found[name]
        if toggle["version"] == version:
            print(f"= {name}: already {version} ({path})")
            continue
        if name not in explicit and is_earlier(toggle["version"], release_version):
            print(f"= {name}: already enabled since {toggle['version']}, kept ({path})")
            continue
        print(f"~ {name}: {toggle['version']} -> {version} ({path})")
        toggle["version"] = version
        changed_files.add(path)

    if check_only:
        return 0
    for path in changed_files:
        with open(path, "w", encoding="utf-8") as f:
            f.write(json.dumps(configs[path], indent=4, ensure_ascii=False) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
