package com.microscope.control.facade;

import java.util.List;
import org.springframework.stereotype.Component;
import com.microscope.control.command.Command;
import com.microscope.control.state.InstrumentState;
import com.microscope.control.state.StateMachine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MicroscopeFacade {

  private final StateMachine stateMachine;
  private final Timer sequenceExecutionTimer;

  public MicroscopeFacade(StateMachine stateMachine, MeterRegistry registry) {
    this.stateMachine = stateMachine;
    this.sequenceExecutionTimer = Timer.builder("microscope.facade.sequence.execution.time")
        .description("Total time taken to execute an entire acquisition sequence")
        .register(registry);
  }

  public void executeSequence(List<Command> sequence) {
    sequenceExecutionTimer.record(() -> {
      for (Command command : sequence) {
        try {
          if (command.describe().startsWith("Move")) {
            stateMachine.transitionTo(InstrumentState.MOVING);
          } else {
            stateMachine.transitionTo(InstrumentState.CAPTURING);
          }

          command.execute();
          stateMachine.transitionTo(InstrumentState.IDLE);

        } catch (Exception e) {
          stateMachine.fault();
          throw e;
        }
      }
    });
  }
}