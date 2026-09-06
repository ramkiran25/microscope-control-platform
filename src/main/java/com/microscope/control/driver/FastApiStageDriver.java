package com.microscope.control.driver;

import com.microscope.control.dto.MoveRequest;
import com.microscope.control.dto.Position;
import com.microscope.control.exception.DriverCommunicationException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Adapter: implements StageDriver by calling the FastAPI simulator's
 * REST endpoints. This is the only class in the codebase that knows
 * the stage is currently "a Python process on localhost:8000" rather
 * than real hardware — swapping to a real stage controller later means
 * writing a new class here, nothing else changes.
 */
@Component
public class FastApiStageDriver implements StageDriver {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient client;
    private volatile boolean connected = false;
    
    @PostConstruct
    public void init() {
        connect();
    }

    public FastApiStageDriver(WebClient simulatorWebClient) {
        this.client = simulatorWebClient;
    }

    @Override
    public void connect() {
        // Simulator is stateless HTTP — "connecting" just means confirming
        // it's reachable before we start accepting commands.
        if (!isHealthy()) {
            throw new DriverCommunicationException("Stage simulator not reachable at startup");
        }
        connected = true;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public boolean isHealthy() {
        try {
            client.get().uri("/stage/position")
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void moveTo(double x, double y, double z) {
        if (!connected) {
            throw new DriverCommunicationException("Stage driver used before connect()");
        }
        try {
            client.post()
                    .uri("/stage/move")
                    .bodyValue(new MoveRequest(x, y, z))
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            throw new DriverCommunicationException(
                    "Stage move rejected by simulator: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new DriverCommunicationException("Stage move failed or timed out", e);
        }
    }

    @Override
    public Position getPosition() {
        try {
            Position position = client.get()
                    .uri("/stage/position")
                    .retrieve()
                    .bodyToMono(Position.class)
                    .block(TIMEOUT);
            if (position == null) {
                throw new DriverCommunicationException("Simulator returned empty position");
            }
            return position;
        } catch (WebClientResponseException e) {
            throw new DriverCommunicationException(
                    "Failed to read stage position: " + e.getStatusCode(), e);
        }
    }
}