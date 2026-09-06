package com.microscope.control.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.microscope.control.driver.CameraDriver;
import com.microscope.control.driver.StageDriver;


public class AcquisitionSequenceBuilder {
  private final StageDriver stageDriver;
  private final CameraDriver cameraDriver;
  List<Command> commands = new ArrayList<>();

  public AcquisitionSequenceBuilder(StageDriver stageDriver, CameraDriver cameraDriver) {
    this.stageDriver = stageDriver;
    this.cameraDriver = cameraDriver;

  }

  public AcquisitionSequenceBuilder moveTo(double x, double y, double z) {
    commands.add(new MoveCommand(stageDriver, x, y, z));
    return this;
  }

  public AcquisitionSequenceBuilder capture(long exposureMs) {
    // CaptureCommand
    commands.add(new CaptureCommand(cameraDriver, exposureMs));
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
