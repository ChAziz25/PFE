package com.PFE.backend.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingResultService {
    private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<String> register(String commandId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(commandId, future);
        return future;
    }

    public void complete(String commandId, String output) {
        CompletableFuture<String> future = pending.remove(commandId);
        if (future != null) future.complete(output);
    }
}
