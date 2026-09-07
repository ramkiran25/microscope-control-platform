"""
Fake stage physics: moves take time proportional to distance, and
occasionally "stall" to simulate a real motor fault. This is what gives
your Java StateMachine/FAULT handling something real to react to,
instead of every call always succeeding.
"""

import math
import random
import time

STALL_PROBABILITY = 0.03  # ~3% of moves simulate a stall, tune as you like
MM_PER_SECOND = 200.0     # simulated stage speed (um/s), keeps demo moves fast


class StageStallError(Exception):
    """Raised to simulate a motor fault partway through a move."""


def simulate_move(current: dict, target_x: float, target_y: float, target_z: float) -> None:
    distance = math.sqrt(
        (target_x - current["x"]) ** 2
        + (target_y - current["y"]) ** 2
        + (target_z - current["z"]) ** 2
    )
    duration = distance / MM_PER_SECOND

    if random.random() < STALL_PROBABILITY:
        # Fail partway through, like a real stall would.
        time.sleep(duration / 2)
        raise StageStallError("Simulated stage stall during move")

    #time.sleep(duration)