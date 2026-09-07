package com.microscope.control.facade;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Component;
import com.microscope.control.command.AcquisitionSequenceBuilder;
import com.microscope.control.command.Command;
import com.microscope.control.command.MoveCommand;
import com.microscope.control.driver.CameraDriver;
import com.microscope.control.driver.StageDriver;
import com.microscope.control.exception.DriverCommunicationException;
import com.microscope.control.state.InstrumentState;
import com.microscope.control.state.StateMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ContinuousRunService {

  private final StageDriver stageDriver;
  private final CameraDriver cameraDriver;
  private final StateMachine stateMachine;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private volatile boolean running = false;

  public ContinuousRunService(StageDriver stageDriver, CameraDriver cameraDriver,
      StateMachine stateMachine) {
    this.stageDriver = stageDriver;
    this.cameraDriver = cameraDriver;
    this.stateMachine = stateMachine;
  }

  public synchronized void start() {
    if (running) {
      return; // already running — ignore a duplicate start click
    }
    running = true;
    executor.submit(this::runLoop);
    log.info("Continuous demo run started");
  }

  public synchronized void stop() {
    running = false;
    log.info("Continuous demo run stop requested");
  }

  public boolean isRunning() {
    return running;
  }

  private void runLoop() {
    double z = 0;
    double direction = 5;

    while (running) {
      try {
        List<Command> sequence = new AcquisitionSequenceBuilder(stageDriver, cameraDriver)
            .moveTo(0, 0, z).capture(150).build();

        for (Command command : sequence) {
          if (!running)
            break;
          InstrumentState next =
              (command instanceof MoveCommand) ? InstrumentState.MOVING : InstrumentState.CAPTURING;
          stateMachine.transitionTo(next);
          command.execute();
          stateMachine.transitionTo(InstrumentState.IDLE);
        }

        // Bounce z back and forth between 0 and 50 — gives the
        // UI visibly changing, non-random-looking movement.
        z += direction;
        if (z >= 50 || z <= 0) {
          direction = -direction;
        }

      } catch (DriverCommunicationException e) {
        log.warn("Continuous demo run hit a simulated fault: {}", e.getMessage());
        stateMachine.fault();
        sleep(1000);
        stateMachine.reset(); // demo-only auto-recovery, see class javadoc
      } catch (Exception e) {
        log.error("Unexpected error in continuous run loop", e);
      }

      sleep(500);
    }
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
