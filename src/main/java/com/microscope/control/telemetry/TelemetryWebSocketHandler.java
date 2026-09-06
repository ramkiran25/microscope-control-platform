package com.microscope.control.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microscope.control.dto.TelemetryUpdate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The outbound side of the Observer relay: subscribes to
 * TelemetryPublisher, and fans every update out to whichever Angular
 * clients currently have a WebSocket connection open. Angular never
 * talks to the simulator or the StateMachine directly — this handler
 * is its only telemetry source.
 */
@Slf4j
@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TelemetryPublisher publisher;

    public TelemetryWebSocketHandler(TelemetryPublisher publisher) {
        this.publisher = publisher;
    }

    @PostConstruct
    public void subscribeToTelemetry() {
        publisher.subscribe(this::broadcast);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Angular client connected to telemetry stream: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
        log.info("Angular client disconnected from telemetry stream: {}", session.getId());
    }

    private void broadcast(TelemetryUpdate update) {
        if (sessions.isEmpty()) {
            return; // nobody listening — don't bother serializing
        }
        try {
            String json = objectMapper.writeValueAsString(update);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast telemetry update: {}", e.getMessage());
        }
    }
}