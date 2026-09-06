package com.microscope.control.command;

import com.microscope.control.driver.StageDriver;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MoveCommand implements Command{
  private final StageDriver stageDriver;
  private final double x;
  private final double y;
  private final double z;
  @Override
  public void execute() {
    stageDriver.moveTo(x, y, z);
    
  }

  @Override
  public String describe() {
    // TODO Auto-generated method stub
    return String.format("Move to (%.2f, %.2f, %.2f)", x, y, z);
  }

}
