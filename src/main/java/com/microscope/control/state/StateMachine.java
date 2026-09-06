package com.microscope.control.state;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import com.microscope.control.exception.IllegalStateTransitionException;
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
    // FAULT only ever goes back to IDLE, and only via reset() below —
    // never as a side effect of a normal command.
    ALLOWED_TRANSITIONS.put(FAULT, EnumSet.noneOf(InstrumentState.class));
  }

  private final AtomicReference<InstrumentState> current = new AtomicReference<>(IDLE);

  public InstrumentState getCurrentState() {
    return current.get();
  }

  /**
   * Attempt to move to a new state. Throws IllegalStateTransitionException if the move isn't legal
   * from the current state — callers (typically a facade or sequence runner) should call this
   * *before* touching any driver, so an illegal command never reaches hardware.
   */
  public void transitionTo(InstrumentState target) {
    InstrumentState previous = current.get();
    Set<InstrumentState> allowed = ALLOWED_TRANSITIONS.get(previous);

    if (allowed == null || !allowed.contains(target)) {
      throw new IllegalStateTransitionException(previous, target);
    }

    if (!current.compareAndSet(previous, target)) {
      // Another thread changed the state between our read and this
      // write — treat it the same as an illegal transition rather
      // than silently overwriting whatever just happened.
      throw new IllegalStateTransitionException(previous, target);
    }
  }

  /**
   * Force a transition into FAULT regardless of the current state. Called from a catch block when a
   * DriverCommunicationException propagates out of a Command — a failure is always allowed to
   * short-circuit whatever was happening.
   */
  public void fault() {
    current.set(FAULT);
  }

  /**
   * Explicit recovery from FAULT back to IDLE. Deliberately separate from transitionTo() —
   * recovering from a fault is an operator decision, not something that should happen as a side
   * effect of an unrelated call.
   */
  public void reset() {
    current.set(IDLE);
  }
}
