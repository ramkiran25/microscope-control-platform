package com.microscope.control.driver;

import com.microscope.control.dto.Position;

public interface StageDriver extends Driver {
  void moveTo(double x, double y, double z);

  Position getPosition();
}
