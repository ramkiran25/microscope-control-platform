package com.microscope.control.command;

import org.springframework.stereotype.Component;
import com.microscope.control.driver.CameraDriver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;


public class CaptureCommand implements Command {
  private final CameraDriver cameraDriver;
  @Getter
  private final long exposureMs;
  private String lastFrameId;
  // Analytics metrics
  private final Timer captureTimer;
  private final Counter captureErrorCounter;

  public CaptureCommand(CameraDriver cameraDriver, long exposureMs, MeterRegistry registry) {
    this.cameraDriver = cameraDriver;
    this.exposureMs = exposureMs;
    this.captureTimer = Timer.builder("microscope.camera.capture.time")
        .description("Execution time for camera captures").register(registry);
    this.captureErrorCounter = Counter.builder("microscope.camera.capture.errors")
        .description("Number of failed camera captures").register(registry);
  }



  @Override
  public void execute() {
    captureTimer.record(() -> {
      try {
        lastFrameId = cameraDriver.capture(exposureMs);
      } catch (Exception e) {
        captureErrorCounter.increment();
        throw e;
      }
    });

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
