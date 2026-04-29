package com.PFE.backend.ai;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.utils.Options;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service("ollama")
public class OllamaProvider implements AiProvider{
    private final OllamaAPI api;

    public OllamaProvider() {
        this.api = new OllamaAPI("http://localhost:11434");
        this.api.setRequestTimeoutSeconds(120);
    }

    @Override
    public String ask(String question, String apiKey) {
        try {
            OllamaResult result = api.generate("qwen2.5-coder:7b", question, false, new Options(new HashMap<>()));
            return result.getResponse();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
