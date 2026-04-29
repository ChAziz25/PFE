package com.PFE.backend.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

@Service("gemini")
public class GeminiProvider implements AiProvider {

    @Override
    public String ask(String question, String apiKey) {
        try {
            Client client = Client.builder().apiKey(apiKey).build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.0-flash",
                    question,
                    null
            );

            return response.text();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
