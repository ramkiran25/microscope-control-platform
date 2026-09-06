from fastapi import APIRouter, HTTPException

from app.models.schemas import MoveRequest, Position
from app.models.state import OutOfBoundsError, simulator_state
from app.simulation.stage_sim import StageStallError, simulate_move

router = APIRouter(prefix="/stage", tags=["stage"])


@router.post("/move", status_code=204)
def move_stage(request: MoveRequest) -> None:
    """
    Move the simulated stage to an absolute position.

    Mirrors what real stage firmware does: validate bounds first, then
    block until the move physically completes (or raise if it stalls).
    Java's FastApiStageDriver.moveTo() calls this and blocks on the
    HTTP response, so the timing here directly affects sequence timing
    on the Java side too.
    """
    current = simulator_state.get_position()

    try:
        simulate_move(current, request.x, request.y, request.z)
    except StageStallError as e:
        raise HTTPException(status_code=503, detail=str(e))

    try:
        simulator_state.validate_and_set_position(request.x, request.y, request.z)
    except OutOfBoundsError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/position", response_model=Position)
def get_stage_position() -> Position:
    pos = simulator_state.get_position()
    return Position(**pos)