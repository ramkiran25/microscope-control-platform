"""
Field names here are chosen to match the Java DTOs exactly
(com.microscope.control.dto.*), so Jackson and Pydantic serialize/deserialize
the same JSON shape with zero extra mapping code on either side.
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel


class MoveRequest(BaseModel):
    x: float
    y: float
    z: float


class Position(BaseModel):
    x: float
    y: float
    z: float


class CaptureRequest(BaseModel):
    exposureMs: int


class CameraState(str, Enum):
    IDLE = "IDLE"
    EXPOSING = "EXPOSING"
    FAULT = "FAULT"


class CameraStatus(BaseModel):
    state: CameraState
    lastFrameId: Optional[str] = None