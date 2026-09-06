from fastapi import FastAPI
from app.websocket import telemetry
from app.routers import camera, stage

app = FastAPI(
    title="Microscope Hardware Simulator",
    description="Stands in for real stage/camera hardware behind a REST API, "
                "so the Java control service can be developed and tested "
                "without a physical instrument.",
    version="0.1.0",
)

app.include_router(stage.router)
app.include_router(camera.router)
app.include_router(telemetry.router)


@app.get("/health")
def health() -> dict:
    """Basic liveness probe, separate from the per-device health checks
    the Java drivers use (GET /stage/position, GET /camera/status)."""
    return {"status": "ok"}