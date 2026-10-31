package com.ias.collector;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ias.engine.RuleEngine;
import com.ias.repository.LogEventRepository;
import jakarta.annotation.PostConstruct;

/**
 * Every 2 seconds, checks the log file for new lines.
 * It remembers how far it has read, so each line is processed only once.
 */
@Component
public class LogFileCollector {

    private static final Logger log = LoggerFactory.getLogger(LogFileCollector.class);

    @Value("${ias.log.source-path}")
    private String logPath;

    private final LogParser parser;
    private final LogEventRepository eventRepository;
    private final RuleEngine ruleEngine;

    private long position = 0;   // how many bytes we have already read

    public LogFileCollector(LogParser parser, LogEventRepository eventRepository, RuleEngine ruleEngine) {
        this.parser = parser;
        this.eventRepository = eventRepository;
        this.ruleEngine = ruleEngine;
    }

    // On startup, skip old content and only watch for new lines
    @PostConstruct
    public void start() {
        File file = new File(logPath);
        position = file.exists() ? file.length() : 0;
        log.info("Watching log file: {}", logPath);
    }

    @Scheduled(fixedDelay = 2000)
    public void readNewLines() {
        File file = new File(logPath);
        if (!file.exists()) {
            return;
        }
        if (file.length() < position) {
            position = 0;   // the file was cleared or replaced, start over
        }

        try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
            reader.seek(position);
            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(line);
            }
            position = reader.getFilePointer();
        } catch (IOException e) {
            log.error("Could not read log file", e);
        }
    }

    private void handleLine(String line) {
        if (line.isBlank()) {
            return;
        }
        try {
            parser.parse(line).ifPresent(event -> {
                eventRepository.save(event);   // 1. store the event
                ruleEngine.run(event);         // 2. check it against all rules
            });
        } catch (Exception e) {
            log.warn("Skipped a bad line: {}", e.getMessage());
        }
    }
}