from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

SPLINE_RE = re.compile(
    r" SPLINE id=(\S+) world=(\S+) loop=(true|false) length=([\d.]+) n=(\d+)"
    r" first=([\d.-]+),([\d.-]+),([\d.-]+) last=([\d.-]+),([\d.-]+),([\d.-]+)"
)
PT_RE = re.compile(
    r" PT spline=(\S+) s=([\d.]+) ([\d.-]+),([\d.-]+),([\d.-]+)"
)
JUNCTION_RE = re.compile(
    r" JUNCTION id=(\S+) stem=(\S+) s=([\d.]+) frog=(\S+) side=(\S+) facing=(-?\d+)"
    r" branch=(\S+) branchLen=(\S+) branchTip=(\S+)"
)
DROP_RE = re.compile(r" JUNCTION_DROP id=(\S+) stem=(\S+) reason=(\S+)")
XYZ_RE = re.compile(r"^([\d.-]+),([\d.-]+),([\d.-]+)$")
TIP_RE = re.compile(r"^([\d.-]+),([\d.-]+)$")


@dataclass
class Pt:
    s: float
    x: float
    y: float
    z: float


@dataclass
class Spline:
    id: str
    world: str
    loop: bool
    length: float
    n: int
    first: tuple[float, float, float]
    last: tuple[float, float, float]
    pts: list[Pt] = field(default_factory=list)


@dataclass
class Junction:
    id: str
    stem: str
    s: float
    frog: tuple[float, float, float] | None
    side: str
    facing: int
    branch: str
    branch_len: float | None
    branch_tip: tuple[float, float] | None


@dataclass
class Dump:
    splines: dict[str, Spline]
    junctions: list[Junction]
    drops: list[tuple[str, str, str]]


def _xyz(raw: str) -> tuple[float, float, float] | None:
    if raw == "-":
        return None
    m = XYZ_RE.match(raw)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2)), float(m.group(3))


def _tip(raw: str) -> tuple[float, float] | None:
    if raw == "-":
        return None
    m = TIP_RE.match(raw)
    if not m:
        return None
    return float(m.group(1)), float(m.group(2))


def parse_track_log(path: Path) -> Dump:
    dumps: list[Dump] = []
    current: Dump | None = None
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if " DUMP " in line:
            current = Dump(splines={}, junctions=[], drops=[])
            dumps.append(current)
            continue
        if current is None:
            continue
        m = SPLINE_RE.search(line)
        if m:
            current.splines[m.group(1)] = Spline(
                id=m.group(1),
                world=m.group(2),
                loop=m.group(3) == "true",
                length=float(m.group(4)),
                n=int(m.group(5)),
                first=(float(m.group(6)), float(m.group(7)), float(m.group(8))),
                last=(float(m.group(9)), float(m.group(10)), float(m.group(11))),
            )
            continue
        m = PT_RE.search(line)
        if m:
            spline = current.splines.get(m.group(1))
            if spline is None:
                continue
            spline.pts.append(
                Pt(
                    s=float(m.group(2)),
                    x=float(m.group(3)),
                    y=float(m.group(4)),
                    z=float(m.group(5)),
                )
            )
            continue
        m = JUNCTION_RE.search(line)
        if m:
            blen = None if m.group(8) == "-" else float(m.group(8))
            current.junctions.append(
                Junction(
                    id=m.group(1),
                    stem=m.group(2),
                    s=float(m.group(3)),
                    frog=_xyz(m.group(4)),
                    side=m.group(5),
                    facing=int(m.group(6)),
                    branch=m.group(7),
                    branch_len=blen,
                    branch_tip=_tip(m.group(9)),
                )
            )
            continue
        m = DROP_RE.search(line)
        if m:
            current.drops.append((m.group(1), m.group(2), m.group(3)))
    if not dumps:
        raise SystemExit(f"No DUMP block in {path}")
    return dumps[-1]


def short_id(uid: str) -> str:
    return uid.split("-")[0]


def branch_ids(dump: Dump) -> set[str]:
    return {j.branch for j in dump.junctions if j.branch and j.branch != "null"}
