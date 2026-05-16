package com.PFE.backend.ai;

import com.PFE.backend.services.CommandProducerService;
import com.PFE.backend.services.PendingResultService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service("gemini")
public class GeminiProvider implements AiProvider {
    private final CommandProducerService commandProducerService;
    private final PendingResultService pendingResultService;

    public GeminiProvider(CommandProducerService commandProducerService, PendingResultService pendingResultService) {
        this.commandProducerService = commandProducerService;
        this.pendingResultService = pendingResultService;
    }

    @Override
    public String ask(String question, String apiKey, String containerId) {
        try {
            Client client = Client.builder().apiKey(apiKey).build();

            FunctionDeclaration executeCommand = FunctionDeclaration.builder()
                    .name("execute_command")
                    .description("Execute a command in the container")
                    .parameters(Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(ImmutableMap.of(
                                    "command", Schema.builder()
                                            .type(Type.Known.STRING)
                                            .build()
                            ))
                            .required(ImmutableList.of("command"))
                            .build())
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .tools(Collections.singletonList(Tool.builder()
                            .functionDeclarations(ImmutableList.of(executeCommand))
                            .build()))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.1-flash-lite",
                    question,
                    config
            );

            Optional<FunctionCall> functionCall = response.candidates()
                    .flatMap(c -> c.isEmpty() ? Optional.empty() : Optional.of(c.getFirst()))
                    .flatMap(Candidate::content)
                    .flatMap(Content::parts)
                    .flatMap(parts -> parts.stream()
                            .map(Part::functionCall)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst());

            if (functionCall.isPresent()) {
                String command = functionCall.get().args()
                        .map(args -> args.get("command").toString())
                        .orElseThrow();
                String commandID = commandProducerService.sendCommand(containerId, command, "AI");
                CompletableFuture<String> future = pendingResultService.register(commandID);
                return future.get(30, TimeUnit.SECONDS);
            } else {
                return response.text();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
