from __future__ import annotations

import math
import sys
from pathlib import Path

import pygame

from trackmap.parse import Dump, branch_ids, parse_track_log, short_id

TOOLS = Path(__file__).resolve().parent.parent
DEFAULT_LOG = TOOLS / "input" / "track.log"

BG = (18, 22, 28)
GRID_MINOR = (38, 46, 56)
GRID_MAJOR = (58, 70, 86)
GRID_LABEL = (140, 155, 175)
HUD = (230, 236, 245)
HUD_DIM = (160, 172, 188)
LOOP = (70, 160, 255)
BRANCH = (255, 140, 40)
STEM = (90, 210, 130)
FROG = (255, 40, 50)
FROG_RING = (255, 220, 60)
TIP = (255, 180, 80)
CROSS = (255, 255, 255)

MIN_ZOOM = 0.15
MAX_ZOOM = 48.0


def world_bounds(dump: Dump) -> tuple[float, float, float, float]:
    xs: list[float] = []
    zs: list[float] = []
    for spline in dump.splines.values():
        for p in spline.pts:
            xs.append(p.x)
            zs.append(p.z)
    for j in dump.junctions:
        if j.frog:
            xs.append(j.frog[0])
            zs.append(j.frog[2])
        if j.branch_tip:
            xs.append(j.branch_tip[0])
            zs.append(j.branch_tip[1])
    if not xs:
        raise SystemExit("DUMP has no PT samples")
    pad = 40
    return min(xs) - pad, max(xs) + pad, min(zs) - pad, max(zs) + pad


def bresenham(x0: int, y0: int, x1: int, y1: int) -> list[tuple[int, int]]:
    pts: list[tuple[int, int]] = []
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    x, y = x0, y0
    while True:
        pts.append((x, y))
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x += sx
        if e2 < dx:
            err += dx
            y += sy
    return pts


class Camera:
    def __init__(self, dump: Dump, size: tuple[int, int]) -> None:
        self.x0, self.x1, self.z0, self.z1 = world_bounds(dump)
        self.w, self.h = size
        self.zoom = 1.0
        self.origin_x = 0.0
        self.origin_z = 0.0
        self.fit()

    def fit(self) -> None:
        span_x = max(1.0, self.x1 - self.x0)
        span_z = max(1.0, self.z1 - self.z0)
        self.zoom = min(self.w / span_x, self.h / span_z) * 0.92
        self.zoom = max(MIN_ZOOM, min(MAX_ZOOM, self.zoom))
        cx = (self.x0 + self.x1) * 0.5
        cz = (self.z0 + self.z1) * 0.5
        self.origin_x = cx - self.w / (2 * self.zoom)
        self.origin_z = cz - self.h / (2 * self.zoom)

    def world_to_screen(self, x: float, z: float) -> tuple[float, float]:
        sx = (x - self.origin_x) * self.zoom
        sy = (z - self.origin_z) * self.zoom
        return sx, sy

    def screen_to_world(self, sx: float, sy: float) -> tuple[float, float]:
        return sx / self.zoom + self.origin_x, sy / self.zoom + self.origin_z

    def zoom_at(self, mx: float, my: float, factor: float) -> None:
        wx, wz = self.screen_to_world(mx, my)
        self.zoom = max(MIN_ZOOM, min(MAX_ZOOM, self.zoom * factor))
        self.origin_x = wx - mx / self.zoom
        self.origin_z = wz - my / self.zoom

    def pan(self, dx: float, dy: float) -> None:
        self.origin_x -= dx / self.zoom
        self.origin_z -= dy / self.zoom


def draw_pixel(surf: pygame.Surface, x: int, y: int, color: tuple[int, int, int], size: int) -> None:
    w, h = surf.get_size()
    r = max(1, size)
    half = r // 2
    rect = pygame.Rect(x - half, y - half, r, r)
    if rect.right < 0 or rect.bottom < 0 or rect.left > w or rect.top > h:
        return
    surf.fill(color, rect)


def paint_polyline(
    surf: pygame.Surface,
    cam: Camera,
    pts: list[tuple[float, float]],
    color: tuple[int, int, int],
    thick: int,
) -> None:
    if len(pts) < 2:
        return
    screens = [cam.world_to_screen(x, z) for x, z in pts]
    for i in range(len(screens) - 1):
        ax, ay = screens[i]
        bx, by = screens[i + 1]
        for px, py in bresenham(int(ax), int(ay), int(bx), int(by)):
            draw_pixel(surf, px, py, color, thick)


def paint_grid(surf: pygame.Surface, cam: Camera, font: pygame.font.Font) -> None:
    w, h = surf.get_size()
    x0, z0 = cam.screen_to_world(0, 0)
    x1, z1 = cam.screen_to_world(w, h)
    major = 32 if cam.zoom < 6 else 16
    minor = 8
    step = minor if cam.zoom >= 2 else major
    gx0 = int(math.floor(x0 / step) * step)
    gz0 = int(math.floor(z0 / step) * step)
    gx = gx0
    while gx <= x1:
        sx, _ = cam.world_to_screen(gx, 0)
        color = GRID_MAJOR if gx % major == 0 else GRID_MINOR
        pygame.draw.line(surf, color, (int(sx), 0), (int(sx), h), 1)
        if gx % major == 0 and cam.zoom >= 0.6:
            label = font.render(str(int(gx)), True, GRID_LABEL)
            surf.blit(label, (int(sx) + 3, 4))
        gx += step
    gz = gz0
    while gz <= z1:
        _, sy = cam.world_to_screen(0, gz)
        color = GRID_MAJOR if gz % major == 0 else GRID_MINOR
        pygame.draw.line(surf, color, (0, int(sy)), (w, int(sy)), 1)
        if gz % major == 0 and cam.zoom >= 0.6:
            label = font.render(f"Z {int(gz)}", True, GRID_LABEL)
            surf.blit(label, (6, int(sy) + 2))
        gz += step


def spline_color(dump: Dump, spline_id: str, loop: bool) -> tuple[int, int, int]:
    if spline_id in branch_ids(dump):
        return BRANCH
    if loop:
        return LOOP
    return STEM


def run(log_path: Path | None = None) -> None:
    path = log_path or DEFAULT_LOG
    if not path.is_file():
        raise SystemExit(f"Put a DUMP log at {path}")
    dump = parse_track_log(path)
    pygame.init()
    pygame.display.set_caption(f"Track map - {path.name}")
    screen = pygame.display.set_mode((1400, 900), pygame.RESIZABLE)
    font = pygame.font.SysFont("consolas", 16)
    small = pygame.font.SysFont("consolas", 13)
    clock = pygame.time.Clock()
    cam = Camera(dump, screen.get_size())
    dragging = False
    last = (0, 0)

    while True:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit()
                return
            if event.type == pygame.VIDEORESIZE:
                cam.w, cam.h = event.w, event.h
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    pygame.quit()
                    return
                if event.key == pygame.K_r:
                    cam.w, cam.h = screen.get_size()
                    cam.fit()
            if event.type == pygame.MOUSEWHEEL:
                mx, my = pygame.mouse.get_pos()
                cam.zoom_at(mx, my, 1.15 if event.y > 0 else 1 / 1.15)
            if event.type == pygame.MOUSEBUTTONDOWN and event.button in (1, 2, 3):
                dragging = True
                last = event.pos
            if event.type == pygame.MOUSEBUTTONUP and event.button in (1, 2, 3):
                dragging = False
            if event.type == pygame.MOUSEMOTION and dragging:
                dx = event.pos[0] - last[0]
                dy = event.pos[1] - last[1]
                cam.pan(dx, dy)
                last = event.pos

        cam.w, cam.h = screen.get_size()
        screen.fill(BG)
        paint_grid(screen, cam, small)
        thick = max(2, int(cam.zoom * 0.35))
        for spline in dump.splines.values():
            if len(spline.pts) < 2:
                continue
            color = spline_color(dump, spline.id, spline.loop)
            paint_polyline(
                screen,
                cam,
                [(p.x, p.z) for p in spline.pts],
                color,
                thick,
            )
            sx, sy = cam.world_to_screen(spline.pts[0].x, spline.pts[0].z)
            draw_pixel(screen, int(sx), int(sy), CROSS, max(4, thick + 2))
        for j in dump.junctions:
            if j.frog is None:
                continue
            fx, fy, fz = j.frog
            sx, sy = cam.world_to_screen(fx, fz)
            r = max(10, int(cam.zoom * 1.4))
            pygame.draw.circle(screen, FROG_RING, (int(sx), int(sy)), r + 6, 3)
            pygame.draw.circle(screen, FROG, (int(sx), int(sy)), r)
            pygame.draw.line(
                screen, CROSS,
                (int(sx) - r, int(sy) - r), (int(sx) + r, int(sy) + r), 3,
            )
            pygame.draw.line(
                screen, CROSS,
                (int(sx) - r, int(sy) + r), (int(sx) + r, int(sy) - r), 3,
            )
            tag = f"JUNCTION {j.side} s={j.s:.0f} {short_id(j.id)}"
            screen.blit(font.render(tag, True, FROG), (int(sx) + r + 8, int(sy) - 10))
            if j.branch_tip:
                tx, tz = j.branch_tip
                tsx, tsy = cam.world_to_screen(tx, tz)
                pygame.draw.circle(screen, TIP, (int(tsx), int(tsy)), max(5, thick + 2))
                screen.blit(small.render("tip", True, TIP), (int(tsx) + 8, int(tsy) - 8))

        mx, my = pygame.mouse.get_pos()
        wx, wz = cam.screen_to_world(mx, my)
        hud = [
            f"{path.name}  splines={len(dump.splines)}  junctions={len(dump.junctions)}",
            f"cursor X={wx:.1f}  Z={wz:.1f}  zoom={cam.zoom:.2f} px/block",
            "drag: pan   wheel: zoom   R: fit   Esc: quit   X east, Z south (north up)",
        ]
        y = 8
        for line in hud:
            screen.blit(font.render(line, True, HUD if y == 8 else HUD_DIM), (8, y))
            y += 20
        pygame.display.flip()
        clock.tick(60)


if __name__ == "__main__":
    extra = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    run(extra)
