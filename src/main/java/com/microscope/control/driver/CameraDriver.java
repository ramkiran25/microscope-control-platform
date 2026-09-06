package com.microscope.control.driver;

import com.microscope.control.dto.CameraStatus;

public interface CameraDriver extends Driver {

  String capture(long exposureMs);

  CameraStatus getStatus();

}
