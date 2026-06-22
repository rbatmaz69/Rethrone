#!/usr/bin/env python3
"""Diff-Coverage-Gate: prueft, ob die in diesem PR *geaenderten* Kotlin-Zeilen
ausreichend von Tests abgedeckt sind.

Im Gegensatz zum absoluten Kover-Gate (`koverVerifyDebug`, Gesamt-Floor) zwingt dieser
Check, dass **neuer/geaenderter** Code getestet ist – unabhaengig vom Gesamtwert.

Ablauf:
1. `git diff <base>...HEAD` ermitteln (hinzugefuegte/geaenderte Zeilen auf der neuen Seite).
2. Kover-XML (JaCoCo-Format) nach Zeilenabdeckung je Quelldatei parsen.
3. Schnittmenge bilden: nur Zeilen, die sowohl geaendert als auch im Report messbar sind.
4. Quote berechnen; faellt sie unter die Schwelle, Exit-Code 1.

Dateien ohne messbare Zeilen (z. B. reine UI-Composables, die aus Kover ausgeschlossen
sind) zaehlen nicht mit – dort kann das Gate nicht greifen und blockiert daher nicht.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

HUNK_RE = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")


def changed_lines(base: str, path_filter: str) -> dict[str, set[int]]:
    """Liefert je Quelldatei (Repo-relativer Pfad) die hinzugefuegten Zeilennummern."""
    diff = subprocess.run(
        ["git", "diff", "--unified=0", f"{base}...HEAD", "--", path_filter],
        capture_output=True,
        text=True,
        check=True,
    ).stdout

    result: dict[str, set[int]] = {}
    current: str | None = None
    new_lineno = 0
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            result.setdefault(current, set())
            continue
        m = HUNK_RE.match(line)
        if m:
            new_lineno = int(m.group(1))
            continue
        if current is None:
            continue
        if line.startswith("+") and not line.startswith("+++"):
            result[current].add(new_lineno)
            new_lineno += 1
        elif line.startswith("-") and not line.startswith("---"):
            # Geloeschte Zeile: Neuer Zaehler bleibt stehen.
            continue
    return {p: s for p, s in result.items() if s}


def covered_lines(report: str) -> dict[str, dict[int, bool]]:
    """Mappt 'package/Sourcefile.kt' -> {Zeilennummer: abgedeckt?}."""
    root = ET.parse(report).getroot()
    out: dict[str, dict[int, bool]] = {}
    for pkg in root.findall(".//package"):
        pkg_name = pkg.attrib.get("name", "")
        for sf in pkg.findall("sourcefile"):
            key = f"{pkg_name}/{sf.attrib['name']}" if pkg_name else sf.attrib["name"]
            lines = out.setdefault(key, {})
            for ln in sf.findall("line"):
                nr = int(ln.attrib["nr"])
                covered = int(ln.attrib.get("ci", "0")) > 0
                lines[nr] = covered
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("report", help="Pfad zum Kover-XML-Report")
    parser.add_argument("--base", default="origin/main", help="Basis-Ref fuer den Diff")
    parser.add_argument("--threshold", type=float, default=80.0, help="Mindest-Diff-Coverage in %")
    parser.add_argument("--path", default="app/src/main", help="Pfad-Filter fuer den Diff")
    parser.add_argument("--min-lines", type=int, default=5,
                        help="Erst ab so vielen messbaren geaenderten Zeilen greift das Gate")
    args = parser.parse_args()

    changed = changed_lines(args.base, args.path)
    cov = covered_lines(args.report)

    total = 0
    hit = 0
    misses: list[str] = []
    for path, lines in changed.items():
        # Repo-Pfad -> XML-Schluessel: passendes 'package/Sourcefile.kt'-Suffix suchen.
        match_key = next((k for k in cov if path.endswith(k)), None)
        if match_key is None:
            continue
        file_cov = cov[match_key]
        for nr in sorted(lines):
            if nr not in file_cov:
                continue  # nicht messbare Zeile (Kommentar, Signatur, Leerzeile)
            total += 1
            if file_cov[nr]:
                hit += 1
            else:
                misses.append(f"{path}:{nr}")

    print("## 🎯 Diff-Coverage (geaenderte Zeilen)")
    if total < args.min_lines:
        print(f"Nur {total} messbare geaenderte Zeile(n) (< {args.min_lines}) – Gate uebersprungen.")
        return 0

    pct = hit * 100.0 / total
    print(f"Abgedeckt: **{hit}/{total} = {pct:.1f}%** (Schwelle {args.threshold:.0f}%)")
    if misses:
        print("\nUnabgedeckte geaenderte Zeilen:")
        for m in misses[:50]:
            print(f"- `{m}`")

    if pct < args.threshold:
        print(f"\n❌ Diff-Coverage {pct:.1f}% < {args.threshold:.0f}% – bitte Tests fuer neuen Code ergaenzen.")
        return 1
    print(f"\n✅ Diff-Coverage erfuellt.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
