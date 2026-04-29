package com.PFE.backend.services;

import com.PFE.backend.ai.AiProvider;
import com.PFE.backend.models.Secret;
import com.PFE.backend.repositories.SecretRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiAgentService {
    private final SecretRepository secretRepository;

    public AiAgentService(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    @Autowired
    private Map<String, AiProvider> providers;

    public String ask(String provider, String question, String userId) {
        AiProvider ai = providers.get(provider);
        if (ai == null) return "Provider not found: " + provider;

        if (provider.equals("ollama")) return ai.ask(question, null);

        String apiKey = secretRepository.findByUserIdAndName(userId, provider.toUpperCase()+"_API_KEY")
                .map(Secret::getValue)
                .orElseThrow(() -> new RuntimeException("No API key found for " + provider));
        return ai.ask(question, apiKey);
    }
}
