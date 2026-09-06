package com.microscope.control.driver;

import com.microscope.control.dto.CameraStatus;
import com.microscope.control.dto.CaptureRequest;
import com.microscope.control.exception.DriverCommunicationException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Adapter: implements CameraDriver by calling the FastAPI simulator's
 * REST endpoints. Mirrors FastApiStageDriver's structure deliberately —
 * consistent adapters are easier to review and to eventually replace.
 */
@Component
public class FastApiCameraDriver implements CameraDriver {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    // Exposures can legitimately take longer than a status check.
    private static final Duration CAPTURE_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient client;
    private volatile boolean connected = false;

    public FastApiCameraDriver(WebClient simulatorWebClient) {
        this.client = simulatorWebClient;
    }
    
    @PostConstruct
    public void init() {
        connect();
    }

    @Override
    public void connect() {
        if (!isHealthy()) {
            throw new DriverCommunicationException("Camera simulator not reachable at startup");
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
            client.get().uri("/camera/status")
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String capture(long exposureMs) {
        if (!connected) {
            throw new DriverCommunicationException("Camera driver used before connect()");
        }
        try {
            CameraStatus result = client.post()
                    .uri("/camera/capture")
                    .bodyValue(new CaptureRequest(exposureMs))
                    .retrieve()
                    .bodyToMono(CameraStatus.class)
                    .block(CAPTURE_TIMEOUT);

            if (result == null || result.lastFrameId() == null) {
                throw new DriverCommunicationException("Capture did not return a frame id");
            }
            return result.lastFrameId();
        } catch (WebClientResponseException e) {
            throw new DriverCommunicationException(
                    "Capture rejected by simulator: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new DriverCommunicationException("Capture failed or timed out", e);
        }
    }

    @Override
    public CameraStatus getStatus() {
        try {
            CameraStatus status = client.get()
                    .uri("/camera/status")
                    .retrieve()
                    .bodyToMono(CameraStatus.class)
                    .block(TIMEOUT);
            if (status == null) {
                throw new DriverCommunicationException("Simulator returned empty camera status");
            }
            return status;
        } catch (WebClientResponseException e) {
            throw new DriverCommunicationException(
                    "Failed to read camera status: " + e.getStatusCode(), e);
        }
    }
}