// Field names match com.microscope.control.telemetry.TelemetryUpdate exactly,
// same "no mapping code needed" approach used between Java and Python.

export interface Position {
  x: number;
  y: number;
  z: number;
}

export interface TelemetryUpdate {
  position: Position;
  cameraState: "IDLE" | "EXPOSING" | "FAULT";
  lastFrameId: string | null;
  instrumentState: "IDLE" | "MOVING" | "CAPTURING" | "FAULT";
}
