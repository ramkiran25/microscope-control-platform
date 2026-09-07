from fastapi import APIRouter, HTTPException

from app.models.schemas import CameraStatus, CaptureRequest, CameraState
from app.models.state import simulator_state
from app.simulation.camera_sim import CameraFaultError, simulate_capture

router = APIRouter(prefix="/camera", tags=["camera"])


@router.post("/capture", response_model=CameraStatus)
def capture(request: CaptureRequest) -> CameraStatus:
    """
    Trigger an exposure and block until it completes (or raise on a
    simulated fault). Returns the resulting frame id, which
    FastApiCameraDriver.capture() reads to hand back to the caller.
    """
    simulator_state.set_camera_state(CameraState.EXPOSING)

    try:
        simulate_capture(request.exposureMs) # type: ignore
    except CameraFaultError as e:
        simulator_state.set_camera_state(CameraState.FAULT)
        raise HTTPException(status_code=503, detail=str(e))

    frame_id = simulator_state.next_frame_id()
    simulator_state.set_last_frame_id(frame_id)
    simulator_state.set_camera_state(CameraState.IDLE)

    return CameraStatus(state=CameraState.IDLE, lastFrameId=frame_id)


@router.get("/status", response_model=CameraStatus)
def get_camera_status() -> CameraStatus:
    state, frame_id = simulator_state.get_camera_status()
    return CameraStatus(state=state, lastFrameId=frame_id)