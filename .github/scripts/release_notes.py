#!/usr/bin/env python3
"""Builds the GitHub release body from the git history since the previous tag.

Usage: release_notes.py VERSION PREV_TAG REPO > notes.md
Each merged branch becomes one line, grouped by the branch prefix (fix/, feature/, ...).
Direct commits on main are grouped by the first word of the subject.
"""
import re
import subprocess
import sys

version, prev, repo = sys.argv[1], sys.argv[2], sys.argv[3]

SECTIONS = [
    ("Added", ("feature", "feat", "add")),
    ("Changed", ("tweak", "change", "update", "improve", "refactor", "rename")),
    ("Fixed", ("fix", "bugfix", "hotfix")),
    ("Removed", ("remove", "delete", "drop")),
    ("Maintenance", ("docs", "chore", "ci", "test", "tests", "cleanup", "build", "license")),
]


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def section_for(word):
    word = word.lower()
    for title, words in SECTIONS:
        if word in words:
            return title
    return "Changed"


def clean(text):
    text = text.strip().rstrip(".")
    return text[:1].upper() + text[1:] if text else text


rng = f"{prev}..HEAD" if prev else "HEAD"
entries = {title: [] for title, _ in SECTIONS}

for line in git("log", rng, "--first-parent", "--pretty=format:%H%x09%P%x09%s").splitlines():
    sha, parents, subject = line.split("\t", 2)
    if len(parents.split()) > 1:
        m = re.match(r"Merge (?:branch ')?([\w-]+)/([\w./-]+)'?(?::\s*(.*))?$", subject)
        if not m:
            continue
        kind, branch, desc = m.group(1), m.group(2), m.group(3)
        text = desc or git("log", "-1", "--pretty=%s", parents.split()[1]).strip()
        text = re.sub(r"^[\w-]+(?:/[\w-]+)?:\s*", "", text) if not desc else text
        entries[section_for(kind)].append(clean(text))
    else:
        if subject.startswith("Merge") or subject.startswith("Release "):
            continue
        m = re.match(r"([\w-]+)[:\s]\s*(.*)", subject)
        word, text = (m.group(1), m.group(2)) if m else ("change", subject)
        entries[section_for(word)].append(clean(text if word.lower() in sum((w for _, w in SECTIONS), ()) else subject))

out = [f"# RankedSMPX {version}", ""]
changes = False
for title, _ in SECTIONS:
    items = list(dict.fromkeys(entries[title]))
    if items:
        changes = True
        out += [f"## {title}", ""] + [f"- {i}" for i in items] + [""]
if not changes:
    out += ["No notable changes.", ""]

out += [
    "## Download",
    "",
    f"- [Modrinth](https://modrinth.com/plugin/rankedsmpx)",
    f"- [GitHub release](https://github.com/{repo}/releases/tag/v{version}): `RankedSMPX-{version}.jar` below",
    "",
    "## Install",
    "",
    "1. Put the jar in your server's `plugins` folder.",
    "2. Start the server.",
    "3. Edit the files in `plugins/RankedSMPX/`, then run `/rsmp reload`.",
    "",
    "Upgrading keeps your configs and data. Old config files are backed up in `plugins/RankedSMPX/backups/`.",
    "",
    "## Requirements",
    "",
    "- Paper or Folia 1.21.1 or newer",
    "- Java 21 or newer",
    "- Optional: PlaceholderAPI, TAB, LuckPerms",
    "",
    "## Links",
    "",
    f"- [Documentation](https://github.com/{repo}/blob/main/docs/README.md)",
    "- [Discord](https://discord.gg/QGsJYht2vy)",
    f"- [Report a bug](https://github.com/{repo}/issues/new?template=bug_report.yml)",
    f"- [Request a feature](https://github.com/{repo}/issues/new?template=feature_request.yml)",
]
if prev:
    out += ["", f"Full changelog: https://github.com/{repo}/compare/{prev}...v{version}"]
print("\n".join(out))
