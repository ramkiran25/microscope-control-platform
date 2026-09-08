package com.microscope.control.state;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import com.microscope.control.exception.IllegalStateTransitionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import static com.microscope.control.state.InstrumentState.*;

@Component
public class StateMachine {

  private static final Map<InstrumentState, Set<InstrumentState>> ALLOWED_TRANSITIONS =
      new EnumMap<>(InstrumentState.class);

  static {
    ALLOWED_TRANSITIONS.put(InstrumentState.IDLE,
        EnumSet.of(InstrumentState.MOVING, InstrumentState.CAPTURING));
    ALLOWED_TRANSITIONS.put(MOVING, EnumSet.of(IDLE, FAULT));
    ALLOWED_TRANSITIONS.put(CAPTURING, EnumSet.of(IDLE, FAULT));
    ALLOWED_TRANSITIONS.put(FAULT, EnumSet.noneOf(InstrumentState.class));
  }

  private final AtomicReference<InstrumentState> current = new AtomicReference<>(IDLE);
  private final Counter transitionExceptionCounter;
  private final Counter faultCounter;

  public StateMachine(MeterRegistry registry) {
    this.transitionExceptionCounter = Counter.builder("microscope.state.transition.exceptions")
        .description("Count of invalid state transition attempts")
        .register(registry);

    this.faultCounter = Counter.builder("microscope.state.faults")
        .description("Total state machine fault occurrences")
        .register(registry);

    // Real-time gauge representing state: IDLE(0), MOVING(1), FAULT(2), CAPTURING(3)
    Gauge.builder("microscope.instrument.state", current, ref -> ref.get().ordinal())
        .description("Current Instrument State enum ordinal")
        .register(registry);
  }

  public InstrumentState getCurrentState() {
    return current.get();
  }

  public void transitionTo(InstrumentState target) {
    InstrumentState previous = current.get();
    Set<InstrumentState> allowed = ALLOWED_TRANSITIONS.get(previous);

    if (allowed == null || !allowed.contains(target)) {
      transitionExceptionCounter.increment();
      throw new IllegalStateTransitionException(previous, target);
    }

    if (!current.compareAndSet(previous, target)) {
      transitionExceptionCounter.increment();
      throw new IllegalStateTransitionException(previous, target);
    }
  }

  public void fault() {
    faultCounter.increment();
    current.set(FAULT);
  }

  public void reset() {
    current.set(IDLE);
  }
}