package com.microscope.control.exception;

public class DriverCommunicationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DriverCommunicationException(String message) {
    super(message);
  }

  public DriverCommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
