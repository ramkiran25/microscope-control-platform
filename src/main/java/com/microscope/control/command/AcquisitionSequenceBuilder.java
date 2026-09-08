package com.microscope.control.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.microscope.control.driver.CameraDriver;
import com.microscope.control.driver.StageDriver;
import io.micrometer.core.instrument.MeterRegistry;


public class AcquisitionSequenceBuilder {
  private final StageDriver stageDriver;
  private final CameraDriver cameraDriver;
  private final MeterRegistry registry;
  List<Command> commands = new ArrayList<>();

  public AcquisitionSequenceBuilder(StageDriver stageDriver, CameraDriver cameraDriver,MeterRegistry registry) {
    this.stageDriver = stageDriver;
    this.cameraDriver = cameraDriver;
    this.registry = registry;
  }

  public AcquisitionSequenceBuilder moveTo(double x, double y, double z) {
    commands.add(new MoveCommand(stageDriver, x, y, z,registry));
    return this;
  }

  public AcquisitionSequenceBuilder capture(long exposureMs) {
    // CaptureCommand
    commands.add(new CaptureCommand(cameraDriver, exposureMs,registry));
    return this;
  }

  public AcquisitionSequenceBuilder zStack(double x, double y, double startZ, double stepZ,
      int steps, long exposureMs) {
    for (int i = 0; i < steps; i++) {
      moveTo(x, y, startZ + (i * stepZ));
      capture(exposureMs);
    }
    return this;
  }

  // finalize
  public List<Command> build() {
    if (commands.isEmpty()) {
      throw new IllegalStateException("Acquisition sequence has no steps");
    }
    return Collections.unmodifiableList(new ArrayList<>(commands));
  }
}
