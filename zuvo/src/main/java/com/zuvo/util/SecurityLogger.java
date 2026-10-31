package com.zuvo.util;   // keep the package line Eclipse gave you

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes Zuvo's security events to the file the Intrusion Alert System watches.
 */
public class SecurityLogger {

    // Zuvo has its own log file, so it never mixes with other applications
    private static final Path LOG_FILE = Paths.get("C:/logs/zuvo-app.log");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String eventType, String username, String ip, String path) {
        String line = LocalDateTime.now().format(TIME)
                + " " + eventType
                + " user=" + clean(username)
                + " ip=" + clean(ip)
                + " path=" + clean(path)
                + System.lineSeparator();
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Logging must never break Zuvo itself
            System.err.println("Could not write security log: " + e.getMessage());
        }
    }

    // Removes spaces and line breaks so a value can never break the format
    // (also stops attackers from forging fake log lines through a username)
    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String text = value.replaceAll("\\s+", "_");
        return text.length() > 100 ? text.substring(0, 100) : text;
    }
}