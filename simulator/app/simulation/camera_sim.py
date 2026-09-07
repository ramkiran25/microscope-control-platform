"""
Fake camera physics: an exposure actually takes exposureMs, and
occasionally faults, mirroring a real camera's sensor readout errors.
"""

import random
import asyncio

FAULT_PROBABILITY = 0.02


class CameraFaultError(Exception):
    """Raised to simulate a sensor/readout fault during exposure."""


async def simulate_capture(exposure_ms: int) -> None:
    # Convert exposure from milliseconds to seconds
    exposure_sec = exposure_ms / 1000.0

    # Simulate fault condition
    if random.random() < FAULT_PROBABILITY:
        # Partial readout delay before faulting
        await asyncio.sleep(exposure_sec / 2.0)
        raise CameraFaultError("Simulated camera readout fault")

    # Complete exposure duration without blocking the event loop
    await asyncio.sleep(exposure_sec)