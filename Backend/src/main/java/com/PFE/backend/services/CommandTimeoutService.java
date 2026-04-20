package com.PFE.backend.services;

import com.PFE.backend.models.Command;
import com.PFE.backend.repositories.CommandRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandTimeoutService {
    private final CommandRepository commandRepository;

    public CommandTimeoutService(CommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    @Scheduled(fixedRate = 5000)
    public void checkTimeouts(){
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(10);

        List<Command> expiredCommands = commandRepository.findByStatusAndCreatedAtBefore("PENDING", timeoutThreshold);

        for (Command cmd : expiredCommands) {
            cmd.setStatus("FAILED");
            cmd.setOutput("Timeout: command took too long");
            commandRepository.save(cmd);

            System.out.println("Command timed out: "+ cmd.getId());
        }
    }
}
