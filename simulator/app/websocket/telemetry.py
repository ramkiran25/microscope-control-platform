"""
Streams live simulator state over a WebSocket, separate from the
request/response REST endpoints. A real instrument typically has this
same split: commands go request/response, continuous readings
(position, temperature, etc.) go over a persistent stream — this
mirrors that shape even though it's all simulated.
"""

import asyncio

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.models.state import simulator_state

router = APIRouter()

TELEMETRY_INTERVAL_SECONDS = 0.5


@router.websocket("/ws/telemetry")
async def telemetry_stream(websocket: WebSocket) -> None:
    await websocket.accept()
    try:
        while True:
            position = simulator_state.get_position()
            camera_state, last_frame_id = simulator_state.get_camera_status()

            await websocket.send_json({
                "position": position,
                "cameraState": camera_state.value,
                "lastFrameId": last_frame_id,
            })
            await asyncio.sleep(TELEMETRY_INTERVAL_SECONDS)
    except WebSocketDisconnect:
        # Client (the Java service) disconnected — nothing to clean up,
        # simulator_state is shared and unaffected.
        pass