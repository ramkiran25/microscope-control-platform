package com.microscope.control.controller;

import com.microscope.control.command.AcquisitionSequenceBuilder;
import com.microscope.control.command.Command;
import com.microscope.control.command.MoveCommand;
import com.microscope.control.driver.CameraDriver;
import com.microscope.control.driver.StageDriver;
import com.microscope.control.exception.DriverCommunicationException;
import com.microscope.control.exception.IllegalStateTransitionException;
import com.microscope.control.state.InstrumentState;
import com.microscope.control.state.StateMachine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/acquisition")
public class AcquisitionController {

  private final StageDriver stageDriver;
  private final CameraDriver cameraDriver;
  private final StateMachine stateMachine;

  public AcquisitionController(StageDriver stageDriver, CameraDriver cameraDriver,
      StateMachine stateMachine) {
    this.stageDriver = stageDriver;
    this.cameraDriver = cameraDriver;
    this.stateMachine = stateMachine;
  }

  @PostMapping("/reset")
  public ResponseEntity<String> reset() {
    stateMachine.reset();
    return ResponseEntity.ok("Instrument reset to IDLE");
  }

  @PostMapping("/z-stack")
  public ResponseEntity<String> runZStack() {
    List<Command> sequence = new AcquisitionSequenceBuilder(stageDriver, cameraDriver)
        .zStack(0, 0, /* startZ */ 0, /* stepZ */ 5, /* steps */ 4, /* exposureMs */ 200).build();

    try {
      for (Command command : sequence) {
        // Guard the transition BEFORE touching hardware — an illegal
        // request never reaches the driver at all.
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
}
