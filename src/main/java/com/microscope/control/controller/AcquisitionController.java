package com.microscope.control.controller;

import com.microscope.control.command.AcquisitionSequenceBuilder;
import com.microscope.control.command.Command;
import com.microscope.control.command.MoveCommand;
import com.microscope.control.driver.CameraDriver;
import com.microscope.control.driver.StageDriver;
import com.microscope.control.exception.DriverCommunicationException;
import com.microscope.control.exception.IllegalStateTransitionException;
import com.microscope.control.facade.ContinuousRunService;
import com.microscope.control.facade.MicroscopeFacade;
import com.microscope.control.state.InstrumentState;
import com.microscope.control.state.StateMachine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/acquisition")
public class AcquisitionController {

  private final StageDriver stageDriver;
  private final CameraDriver cameraDriver;
  private final StateMachine stateMachine;
  private final ContinuousRunService continuousRunService;
  private final MeterRegistry registry;
  private final MicroscopeFacade microscopeFacade;

  public AcquisitionController(StageDriver stageDriver, CameraDriver cameraDriver,
      StateMachine stateMachine, ContinuousRunService continuousRunService, MeterRegistry registry,
      MicroscopeFacade microscopeFacade) {
    this.stageDriver = stageDriver;
    this.cameraDriver = cameraDriver;
    this.stateMachine = stateMachine;
    this.continuousRunService = continuousRunService;
    this.registry = registry;
    this.microscopeFacade = microscopeFacade;
  }

  @PostMapping("/z-stack")
  public ResponseEntity<String> runZStack() {
    if (continuousRunService.isRunning()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("Rejected: continuous demo run is active — stop it first");
    }

    List<Command> sequence = new AcquisitionSequenceBuilder(stageDriver, cameraDriver, registry)
        .zStack(0, 0, /* startZ */ 0, /* stepZ */ 5, /* steps */ 4, /* exposureMs */ 200).build();
    // Facade encapsulates state transitions, loop execution, and fault management
    microscopeFacade.executeSequence(sequence);

    try {
      for (Command command : sequence) {
        InstrumentState next =
            (command instanceof MoveCommand) ? InstrumentState.MOVING : InstrumentState.CAPTURING;
        stateMachine.transitionTo(next);

        command.execute(); // may throw DriverCommunicationException

        stateMachine.transitionTo(InstrumentState.IDLE);
      }
    } catch (DriverCommunicationException e) {
      stateMachine.fault();
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body("Sequence aborted: " + e.getMessage());
    } catch (IllegalStateTransitionException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Rejected: " + e.getMessage());
    }

    return ResponseEntity.ok("Sequence complete: " + sequence.size() + " steps");
  }

  @PostMapping("/reset")
  public ResponseEntity<String> reset() {
    stateMachine.reset();
    return ResponseEntity.ok("Instrument reset to IDLE");
  }

  @PostMapping("/continuous/start")
  public ResponseEntity<String> startContinuous() {
    continuousRunService.start();
    return ResponseEntity.ok("Continuous demo run started");
  }

  @PostMapping("/continuous/stop")
  public ResponseEntity<String> stopContinuous() {
    continuousRunService.stop();
    return ResponseEntity.ok("Continuous demo run stopped");
  }
}
