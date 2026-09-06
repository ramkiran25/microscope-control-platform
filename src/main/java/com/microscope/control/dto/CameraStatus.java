package com.microscope.control.dto;

public record CameraStatus(CameraState state, String lastFrameId) {
  public enum CameraState {
    IDLE, EXPOSING, FAULT
  }
}
