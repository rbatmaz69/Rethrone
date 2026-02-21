#!/usr/bin/env python3
"""Render Kover XML coverage as markdown or a short overall line."""

from __future__ import annotations

import argparse
import os
import sys
import xml.etree.ElementTree as ET


def parse_counter(node: ET.Element) -> dict[str, tuple[int, int, float]]:
    totals: dict[str, tuple[int, int, float]] = {}
    for counter in node.iter("counter"):
        ctype = counter.attrib["type"]
        missed = int(counter.attrib["missed"])
        covered = int(counter.attrib["covered"])
        total = missed + covered
        pct = 0.0 if total == 0 else covered * 100.0 / total
        totals[ctype] = (covered, total, pct)
    return totals


def parse_report(path: str) -> dict[str, tuple[int, int, float]]:
    if not path or not os.path.exists(path):
        raise FileNotFoundError("Coverage-Report fehlt")
    root = ET.parse(path).getroot()
    return parse_counter(root)


def parse_packages_and_classes(path: str) -> tuple[dict[str, dict[str, tuple[int, int, float]]], dict[str, dict[str, tuple[int, int, float]]]]:
    if not path or not os.path.exists(path):
        raise FileNotFoundError("Coverage-Report fehlt")
    root = ET.parse(path).getroot()

    packages: dict[str, dict[str, tuple[int, int, float]]] = {}
    classes: dict[str, dict[str, tuple[int, int, float]]] = {}

    for pkg in root.findall(".//package"):
        pkg_name = pkg.attrib.get("name", "<default>")
        packages[pkg_name] = parse_counter(pkg)
        for cls in pkg.findall("class"):
            cls_name = cls.attrib.get("name", "<anonymous>")
            classes[f"{pkg_name}.{cls_name}"] = parse_counter(cls)

    return packages, classes


def render_breakdown(title: str, items: dict[str, dict[str, tuple[int, int, float]]], metric: str, top_n: int) -> str:
    rows = []
    for name, totals in items.items():
        covered, total, pct = totals.get(metric, (0, 0, 0.0))
        rows.append((pct, name, covered, total))

    # niedrigste Coverage zuerst, damit Hotspots sichtbar sind
    rows.sort(key=lambda r: (r[0], r[1]))
    rows = rows[:top_n]

    lines = [f"### {title} (Top {top_n}, {metric.capitalize()})", "", "| Name | Covered | Total | Coverage |", "| --- | --- | --- | --- |"]
    for pct, name, covered, total in rows:
        lines.append(f"| {name} | {covered} | {total} | {pct:.2f}% |")
    return "\n".join(lines)


def render_markdown(totals: dict[str, tuple[int, int, float]], packages: dict[str, dict[str, tuple[int, int, float]]], classes: dict[str, dict[str, tuple[int, int, float]]], metric: str, top_n: int) -> str:
    order = ["INSTRUCTION", "LINE", "BRANCH", "METHOD", "CLASS"]
    lines = ["| Metric | Covered | Total | Coverage |", "| --- | --- | --- | --- |"]
    for key in order:
        if key in totals:
            covered, total, pct = totals[key]
            label = key.capitalize()
            lines.append(f"| {label} | {covered} | {total} | {pct:.2f}% |")
    headline = totals.get("LINE") or totals.get("INSTRUCTION")
    overall = f"{headline[2]:.2f}%" if headline else "n/a"

    sections = [f"### Gesamt-Coverage: {overall}", "", *lines]
    if packages:
        sections.extend(["", render_breakdown("Pakete", packages, metric, top_n)])
    if classes:
        sections.extend(["", render_breakdown("Klassen", classes, metric, top_n)])
    return "\n".join(sections)


def render_overall(totals: dict[str, tuple[int, int, float]]) -> str:
    line = totals.get("LINE", (0, 0, 0.0))[2]
    instr = totals.get("INSTRUCTION", (0, 0, 0.0))[2]
    return f"LINE={line:.2f}% INSTRUCTION={instr:.2f}%"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("report", help="Path to Kover XML report")
    parser.add_argument("--overall", action="store_true", help="Print only overall line")
    parser.add_argument("--metric", default="LINE", choices=["LINE", "INSTRUCTION"], help="Metric for package/class ranking")
    parser.add_argument("--top", type=int, default=10, help="Number of package/class rows to show")
    args = parser.parse_args()

    totals = parse_report(args.report)
    if args.overall:
        print(render_overall(totals))
        return 0

    packages, classes = parse_packages_and_classes(args.report)
    print(render_markdown(totals, packages, classes, args.metric, args.top))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

