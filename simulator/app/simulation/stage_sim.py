"""
Fake stage physics: moves take time proportional to distance, and
occasionally "stall" to simulate a real motor fault. This gives
the Java StateMachine/FAULT handling a realistic hardware scenario to react to.
"""

import math
import random
import asyncio

STALL_PROBABILITY = 0.03  # ~3% move stall probability
UM_PER_SECOND = 200.0     # Simulated stage speed in um/s (or mm/s)


class StageStallError(Exception):
    """Raised to simulate a motor fault partway through a move."""


async def simulate_move(current: dict, target_x: float, target_y: float, target_z: float) -> None:
    # 1. Calculate 3D Euclidean distance
    dx = target_x - current["x"]
    dy = target_y - current["y"]
    dz = target_z - current["z"]
    distance = math.sqrt(dx**2 + dy**2 + dz**2)

    if distance <= 0:
        return

    duration = distance / UM_PER_SECOND

    # 2. Handle simulated motor stall (partway through)
    if random.random() < STALL_PROBABILITY:
        await asyncio.sleep(duration / 2.0)
        
        # Move state to the half-way point where motor stalled
        current["x"] += dx / 2.0
        current["y"] += dy / 2.0
        current["z"] += dz / 2.0
        
        raise StageStallError(
            f"Stage stall detected at position ({current['x']:.2f}, {current['y']:.2f}, {current['z']:.2f})"
        )

    # 3. Complete move successfully
    await asyncio.sleep(duration)
    current["x"] = target_x
    current["y"] = target_y
    current["z"] = target_z