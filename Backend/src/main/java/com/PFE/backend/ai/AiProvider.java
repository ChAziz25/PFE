package com.PFE.backend.ai;

public interface AiProvider {
    String ask(String question, String apiKey, String containerId);
}
