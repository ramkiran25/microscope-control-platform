package com.microscope.control.dto;

public record TelemetryUpdate(Position position, String cameraState, String lastFrameId,
    String instrumentState) {

}
