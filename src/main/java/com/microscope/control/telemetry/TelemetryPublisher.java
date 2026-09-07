package com.microscope.control.telemetry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import com.microscope.control.dto.TelemetryUpdate;

@Component
public class TelemetryPublisher {
  private final List<Consumer<TelemetryUpdate>> subscribers = new CopyOnWriteArrayList<>();
  
  public void subscribe(Consumer<TelemetryUpdate> subscriber) {
      subscribers.add(subscriber);
  }

  public void publish(TelemetryUpdate update) {
      for (Consumer<TelemetryUpdate> subscriber : subscribers) {
          subscriber.accept(update);
      }
  }
}
