package com.microscope.control.exception;

import com.microscope.control.state.InstrumentState;

public class IllegalStateTransitionException extends RuntimeException {
  
  private static final long serialVersionUID = 1L;

  public IllegalStateTransitionException(InstrumentState from, InstrumentState to) {
    super(String.format("Cannot transition from %s to %s", from, to));
  }
}
