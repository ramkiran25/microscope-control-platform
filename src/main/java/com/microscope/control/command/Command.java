package com.microscope.control.command;

public interface Command {
  void execute();

  String describe();
}
