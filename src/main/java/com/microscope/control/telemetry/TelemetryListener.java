package com.microscope.control.telemetry;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microscope.control.dto.TelemetryUpdate;
import com.microscope.control.state.InstrumentState;
import com.microscope.control.state.StateMachine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TelemetryListener extends TextWebSocketHandler {

  private final TelemetryPublisher publisher;
  private final StateMachine stateMachine;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String simulatorWsUrl;

  public TelemetryListener(TelemetryPublisher publisher, StateMachine stateMachine,
      @Value("${simulator.telemetry-ws-url:ws://localhost:8000/ws/telemetry}") String simulatorWsUrl) {
    this.publisher = publisher;
    this.stateMachine = stateMachine;
    this.simulatorWsUrl = simulatorWsUrl;
  }

  @PostConstruct
  public void connect() {
    try {
      StandardWebSocketClient client = new StandardWebSocketClient();
      client.execute(this, simulatorWsUrl);
      log.info("Connecting telemetry listener to {}", simulatorWsUrl);
    } catch (Exception e) {
      log.error("Could not connect telemetry listener to simulator: {}", e.getMessage());
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Map<String, Object> raw = objectMapper.readValue(message.getPayload(), Map.class);

    var position =
        objectMapper.convertValue(raw.get("position"), com.microscope.control.dto.Position.class);
    String cameraState = (String) raw.get("cameraState");
    String lastFrameId = (String) raw.get("lastFrameId");

    InstrumentState currentState = stateMachine.getCurrentState();

    TelemetryUpdate update =
        new TelemetryUpdate(position, cameraState, lastFrameId, currentState.name());

    publisher.publish(update);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.warn("Telemetry WebSocket transport error: {}", exception.getMessage());
  }
}
