"""
In-memory state standing in for real hardware registers. A real
instrument driver would read/write these from firmware; here it's just
a dict-like object guarded by locks so concurrent requests don't corrupt
it. One SimulatorState instance lives for the lifetime of the process
and is shared by both routers via FastAPI's dependency system.
"""

import threading

from app.models.schemas import CameraState

# Stage travel limits, in micrometers — used to reject out-of-range moves,
# the same way real stage firmware enforces soft limits.
STAGE_BOUNDS = {
    "x": (0.0, 500.0),
    "y": (0.0, 500.0),
    "z": (0.0, 200.0),
}


class OutOfBoundsError(Exception):
    """Raised when a requested move exceeds the simulated stage's travel limits."""


class SimulatorState:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.position = {"x": 0.0, "y": 0.0, "z": 0.0}
        self.camera_state = CameraState.IDLE
        self.last_frame_id: str | None = None
        self._frame_counter = 0

    def validate_and_set_position(self, x: float, y: float, z: float) -> None:
        for axis, value in (("x", x), ("y", y), ("z", z)):
            low, high = STAGE_BOUNDS[axis]
            if not (low <= value <= high):
                raise OutOfBoundsError(
                    f"{axis}={value} is outside stage bounds [{low}, {high}]"
                )
        with self._lock:
            self.position = {"x": x, "y": y, "z": z}

    def get_position(self) -> dict:
        with self._lock:
            return dict(self.position)

    def next_frame_id(self) -> str:
        with self._lock:
            self._frame_counter += 1
            return f"frame-{self._frame_counter:05d}"

    def set_camera_state(self, state: CameraState) -> None:
        with self._lock:
            self.camera_state = state

    def set_last_frame_id(self, frame_id: str) -> None:
        with self._lock:
            self.last_frame_id = frame_id

    def get_camera_status(self) -> tuple[CameraState, str | None]:
        with self._lock:
            return self.camera_state, self.last_frame_id


# Single shared instance for the whole process — there is exactly one
# simulated instrument, so exactly one state object.
simulator_state = SimulatorState()