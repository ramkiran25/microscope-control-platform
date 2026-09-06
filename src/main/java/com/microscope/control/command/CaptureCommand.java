package com.microscope.control.command;

import com.microscope.control.driver.CameraDriver;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CaptureCommand implements Command {
  private final CameraDriver cameraDriver;
  private final long exposureMs;
  String lastFrameId;
  
  public CaptureCommand(CameraDriver cameraDriver,long exposureMs){
    this.cameraDriver = cameraDriver;
    this.exposureMs = exposureMs;
  }

  @Override
  public void execute() {
    lastFrameId = cameraDriver.capture(exposureMs);

  }

  @Override
  public String describe() {
    // TODO Auto-generated method stub
    return String.format("Capture (exposure %d ms)", exposureMs);
  }

  public String getLastFrameId() {
    return lastFrameId;
  }

}
