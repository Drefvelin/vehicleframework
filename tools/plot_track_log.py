#!/usr/bin/env python3
"""Open a pygame window and paint track.log DUMP pixels (pan / zoom / grid)."""

from __future__ import annotations

import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from trackmap.app import run

if __name__ == "__main__":
    log = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    run(log)
