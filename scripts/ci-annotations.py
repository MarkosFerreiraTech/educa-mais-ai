#!/usr/bin/env python3
"""Publica as falhas de teste como annotations do GitHub Actions.

Le os relatorios XML do surefire/failsafe e imprime linhas no formato
``::error title=<Classe.metodo>::<mensagem>``, que o runner transforma em
annotations consultaveis pela API (usado pelo scripts/ci-doctor.sh).
"""
from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    total = 0
    for path in sorted(glob.glob("backend/target/*-reports/*.xml")):
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            continue
        for case in root.iter("testcase"):
            for kind in ("failure", "error"):
                node = case.find(kind)
                if node is None:
                    continue
                message = " ".join((node.get("message") or "").split())[:230]
                classname = (case.get("classname") or "?").split(".")[-1]
                name = f"{classname}.{case.get('name', '?')}"
                print(f"::error title={name}::{message}")
                total += 1
                break
    if total == 0:
        print("::warning title=CI::nenhuma falha de teste nos XML (verifique a compilacao)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
