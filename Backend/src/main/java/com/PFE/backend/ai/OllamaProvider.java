package com.PFE.backend.ai;

import com.PFE.backend.services.CommandProducerService;
import com.PFE.backend.services.PendingResultService;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatResult;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service("ollama")
public class OllamaProvider implements AiProvider{
    private final OllamaAPI api;
    String systemPrompt = """
    You are an agent that controls a Linux container.
    If the user asks you to do anything involving files, directories, or running commands,
    respond with ONLY this JSON and nothing else:
    {"action": "execute_command", "command": "<the command to run>"}
    Do not add explanation. Do not add markdown. Just the raw JSON.
    If no command is needed, respond normally as plain text.
    """;

    private final CommandProducerService commandProducerService;
    private final PendingResultService pendingResultService;

    public OllamaProvider(CommandProducerService commandProducerService, PendingResultService pendingResultService) {
        this.api = new OllamaAPI("http://localhost:11434");
        this.api.setRequestTimeoutSeconds(120);
        this.commandProducerService = commandProducerService;
        this.pendingResultService = pendingResultService;
    }

    @Override
    public String ask(String question, String apiKey, String containerId) {
        try {
            List<OllamaChatMessage> messages = new ArrayList<>();
            messages.add(new OllamaChatMessage(OllamaChatMessageRole.SYSTEM, systemPrompt));
            messages.add(new OllamaChatMessage(OllamaChatMessageRole.USER, question));

            OllamaChatResult result = api.chat("qwen2.5-coder:7b", messages);
            String responseText = result.getResponse().trim();

            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("```[a-z]*\\n?", "").trim();
            }

            if (responseText.startsWith("{") && responseText.contains("execute_command")) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode json = mapper.readTree(responseText);
                    String command = json.get("command").asText();

                    System.out.println(command);
                    String commandId = commandProducerService.sendCommand(containerId, command, "AI");
                    CompletableFuture<String> future = pendingResultService.register(commandId);
                    return future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return "Error parsing command: " + e.getMessage();
                }
            } else {
                return responseText;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
