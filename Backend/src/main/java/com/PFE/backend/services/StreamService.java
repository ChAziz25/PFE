package com.PFE.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamService {
    public final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter (String commandId) {
        SseEmitter emitter = new SseEmitter(0l);

        emitters.put(commandId, emitter);

        emitter.onCompletion(() -> emitters.remove(commandId));
        emitter.onTimeout(() -> emitters.remove(commandId));
        emitter.onError((e) -> emitters.remove(commandId));

        return emitter;
    }

    public void sendResult(String commandId, String output) {
        SseEmitter emitter = emitters.get(commandId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(output));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
