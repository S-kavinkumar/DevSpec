package com.devspec.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(ProgressRegistryService.class);

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String analysisId) {
        logger.info("Registering real-time progress SSE stream emitter for run: {}", analysisId);
        // Timeout set to 30 minutes
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.put(analysisId, emitter);

        emitter.onCompletion(() -> emitters.remove(analysisId));
        emitter.onTimeout(() -> emitters.remove(analysisId));
        emitter.onError((ex) -> emitters.remove(analysisId));

        // Send handshake event
        try {
            emitter.send(SseEmitter.event().name("handshake").data("Connection established"));
        } catch (Exception e) {
            emitters.remove(analysisId);
        }

        return emitter;
    }

    public void publish(String analysisId, Map<String, Object> progressPayload) {
        SseEmitter emitter = emitters.get(analysisId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(progressPayload));
            } catch (Exception e) {
                logger.warn("Failed to send SSE progress update to {}. Removing emitter.", analysisId);
                emitters.remove(analysisId);
            }
        }
    }

    public void complete(String analysisId) {
        SseEmitter emitter = emitters.remove(analysisId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
