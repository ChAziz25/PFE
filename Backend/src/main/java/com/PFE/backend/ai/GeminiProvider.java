package com.PFE.backend.ai;

import com.PFE.backend.services.CommandProducerService;
import com.PFE.backend.services.PendingResultService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                    .description("Execute a shell command in the Linux container. Use this to explore /tools/, read files, and run scripts.")
                    .parameters(Schema.builder()
                            .type(Type.Known.OBJECT)
                            .properties(ImmutableMap.of(
                                    "command", Schema.builder()
                                            .type(Type.Known.STRING)
                                            .description("The shell command to execute")
                                            .build()
                            ))
                            .required(ImmutableList.of("command"))
                            .build())
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.builder()
                            .role("system")
                            .parts(ImmutableList.of(Part.builder()
                                    .text("You are an agent controlling a Linux container. " +
                                            "When asked to use a tool, always: 1) run ls /tools/ to discover files, " +
                                            "2) cat the file to read it, 3) execute it with python3. " +
                                            "Never guess filenames. Always discover first.")
                                    .build()))
                            .build())
                    .tools(Collections.singletonList(Tool.builder()
                            .functionDeclarations(ImmutableList.of(executeCommand))
                            .build()))
                    .build();

            List<Content> contents = new ArrayList<>();
            contents.add(Content.builder()
                    .role("user")
                    .parts(ImmutableList.of(Part.builder().text(question).build()))
                    .build());

            for (int i = 0; i < 5; i++) {
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-3.1-flash-lite",
                        contents,
                        config
                );

                // Add model response to history
                response.candidates()
                        .flatMap(c -> c.isEmpty() ? Optional.empty() : Optional.of(c.getFirst()))
                        .flatMap(Candidate::content)
                        .ifPresent(contents::add);

                // Check for function call
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

                    System.out.println("[Gemini AI executing]: " + command);
                    String commandId = commandProducerService.sendCommand(containerId, command, "AI");
                    CompletableFuture<String> future = pendingResultService.register(commandId);
                    String commandResult = future.get(30, TimeUnit.SECONDS);
                    System.out.println("[Gemini command result]: " + commandResult);

                    // Feed result back as function response
                    contents.add(Content.builder()
                            .role("user")
                            .parts(ImmutableList.of(Part.builder()
                                    .functionResponse(FunctionResponse.builder()
                                            .name("execute_command")
                                            .response(ImmutableMap.of("output", commandResult))
                                            .build())
                                    .build()))
                            .build());
                } else {
                    return response.text();
                }
            }

            return "Max iterations reached without a final answer.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}