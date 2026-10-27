#!/usr/bin/env python3
"""Etap 9: liczy napisy z polskimi znakami w kodzie UI i treści – mapa pracy do tłumaczenia (docs/localization.md)."""
import pathlib, re, sys, collections

ROOT = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/java/com/example"
pattern = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')
polish = re.compile(r"[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]|\b(i|w|z|na|do|się|nie|jest)\b")

counts = collections.Counter()
for path in ROOT.rglob("*.kt"):
    for s in pattern.findall(path.read_text(encoding="utf-8")):
        if len(s) > 2 and polish.search(s):
            counts[str(path.relative_to(ROOT))] += 1

total = sum(counts.values())
for f, n in counts.most_common():
    print(f"{n:5d}  {f}")
print(f"{total:5d}  RAZEM")
sys.exit(0)
