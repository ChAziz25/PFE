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
    You are an agent controlling a Linux container.
    You MUST keep running commands until you have a final answer for the user.
    To run a command respond with ONLY this JSON, nothing else:
    {"action": "execute_command", "command": "<command>"}
    
    When asked to use a tool in /tools/:
    1. {"action": "execute_command", "command": "ls /tools/"}
    2. {"action": "execute_command", "command": "cat /tools/<filename>"}
    3. {"action": "execute_command", "command": "python3 /tools/<filename> <args>"}
    
    Only respond in plain text when you have the FINAL answer to give the user.
    Never output a filename alone. Never stop mid-task.
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

            String finalResponse = null;
            int maxIterations = 5;

            for (int i = 0; i < maxIterations; i++) {
                OllamaChatResult result = api.chat("qwen2.5-coder:7b", messages);
                String responseText = result.getResponse().trim();

                if (responseText.startsWith("```")) {
                    responseText = responseText.replaceAll("```[a-z]*\\n?", "").trim();
                }

                // Add assistant message to history
                messages.add(new OllamaChatMessage(OllamaChatMessageRole.ASSISTANT, responseText));

                if (responseText.startsWith("{") && responseText.contains("execute_command")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode json = mapper.readTree(responseText);
                    String command = json.get("command").asText();

                    System.out.println("[AI executing]: " + command);
                    String commandId = commandProducerService.sendCommand(containerId, command, "AI");
                    CompletableFuture<String> future = pendingResultService.register(commandId);
                    String commandResult = future.get(30, TimeUnit.SECONDS);

                    System.out.println("[Command result]: " + commandResult);

                    // Feed result back into the conversation
                    boolean looksLikeFinalResult = commandResult.matches("[\\d.\\-]+") || commandResult.lines().count() <= 2;

                    String nudge = looksLikeFinalResult
                            ? "Command output:\n" + commandResult + "\nThis is the result. Give the user the final answer now."
                            : "Command output:\n" + commandResult + "\nYou have not finished yet. What is your next command?";

                    messages.add(new OllamaChatMessage(OllamaChatMessageRole.USER, nudge));
                } else {
                    // No command needed, this is the final answer
                    finalResponse = responseText;
                    break;
                }
            }

            return finalResponse != null ? finalResponse : "Max iterations reached without a final answer.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
