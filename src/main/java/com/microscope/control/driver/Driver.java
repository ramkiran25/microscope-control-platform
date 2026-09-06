package com.microscope.control.driver;

public interface Driver {
  void connect();

  void disconnect();

  boolean isHealthy();
}
