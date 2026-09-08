package com.microscope.control.command;

import org.springframework.stereotype.Component;
import com.microscope.control.driver.StageDriver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;


public class MoveCommand implements Command {
  // Analytics metrics
  private final StageDriver stageDriver;
  private final Counter moveErrorCounter;
  private final Timer moveTimer;
  private final double x;
  private final double y;
  private final double z;

  public MoveCommand(StageDriver stageDriver, double x, double y, double z,
      MeterRegistry registry) {

    this.stageDriver = stageDriver;
    this.x = x;
    this.y = y;
    this.z = z;
    this.moveTimer = Timer.builder("microscope.stage.move.time")
        .description("Execution time for stage movements").register(registry);

    this.moveErrorCounter = Counter.builder("microscope.stage.move.errors")
        .description("Number of failed stage movements").register(registry);
    // Capture overall target displacement magnitude
    registry.summary("microscope.stage.target.distance").record(Math.sqrt(x * x + y * y + z * z));
  }


  @Override
  public void execute() {
    moveTimer.record(() -> {
      try {
        stageDriver.moveTo(x, y, z);
      } catch (Exception e) {
        moveErrorCounter.increment();
        throw e;
      }
    });

  }

  @Override
  public String describe() {
    // TODO Auto-generated method stub
    return String.format("Move to (%.2f, %.2f, %.2f)", x, y, z);
  }

}
