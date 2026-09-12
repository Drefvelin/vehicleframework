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
    r" JUNCTION id=(?P<id>\S+) stem=(?P<stem>\S+) s=(?P<s>[\d.]+) frog=(?P<frog>\S+)"
    r" side=(?P<side>\S+) facing=(?P<facing>-?\d+)"
    r"(?: thrown=(?P<thrown>true|false))?"
    r" branch=(?P<branch>\S+) branchLen=(?P<branchLen>\S+) branchTip=(?P<branchTip>\S+)"
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
    thrown: bool | None
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
            blen_raw = m.group("branchLen")
            thrown_raw = m.group("thrown")
            current.junctions.append(
                Junction(
                    id=m.group("id"),
                    stem=m.group("stem"),
                    s=float(m.group("s")),
                    frog=_xyz(m.group("frog")),
                    side=m.group("side"),
                    facing=int(m.group("facing")),
                    thrown=None if thrown_raw is None else thrown_raw == "true",
                    branch=m.group("branch"),
                    branch_len=None if blen_raw == "-" else float(blen_raw),
                    branch_tip=_tip(m.group("branchTip")),
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


def spline_tangent_xz(spline: Spline, s: float) -> tuple[float, float] | None:
    pts = spline.pts
    if len(pts) < 2:
        dx = spline.last[0] - spline.first[0]
        dz = spline.last[2] - spline.first[2]
        if abs(dx) < 1e-9 and abs(dz) < 1e-9:
            return None
        return dx, dz
    for i in range(len(pts) - 1):
        a = pts[i]
        b = pts[i + 1]
        if s < a.s - 1e-9:
            break
        if s <= b.s + 1e-9 or i == len(pts) - 2:
            dx = b.x - a.x
            dz = b.z - a.z
            if abs(dx) > 1e-9 or abs(dz) > 1e-9:
                return dx, dz
    dx = pts[-1].x - pts[-2].x
    dz = pts[-1].z - pts[-2].z
    if abs(dx) < 1e-9 and abs(dz) < 1e-9:
        return None
    return dx, dz
