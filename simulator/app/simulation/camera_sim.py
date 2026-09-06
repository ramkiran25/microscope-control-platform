"""
Fake camera physics: an exposure actually takes exposureMs, and
occasionally faults, mirroring a real camera's sensor readout errors.
"""

import random
import time

FAULT_PROBABILITY = 0.02


class CameraFaultError(Exception):
    """Raised to simulate a sensor/readout fault during exposure."""


def simulate_capture(exposure_ms: int) -> None:
    if random.random() < FAULT_PROBABILITY:
        time.sleep((exposure_ms / 1000.0) / 2)
        raise CameraFaultError("Simulated camera readout fault")

    time.sleep(exposure_ms / 1000.0)