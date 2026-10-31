package com.ias.collector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ias.model.LogEvent;

/**
 * Understands lines like:
 *   2026-10-21 10:00:01 LOGIN_FAILED user=admin ip=192.168.1.5
 */
@Component
public class LogParser {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // date+time, then the event type, then everything else
    private static final Pattern LINE_PATTERN =
            Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s*(.*)$");

    public Optional<LogEvent> parse(String line) {
        String text = line.trim();
        Matcher matcher = LINE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return Optional.empty();   // not a line we understand, skip it
        }

        Map<String, String> fields = readFields(matcher.group(3));

        LogEvent event = new LogEvent();
        event.setEventTime(LocalDateTime.parse(matcher.group(1), TIME_FORMAT));
        event.setEventType(matcher.group(2));
        event.setUsername(fields.get("user"));
        event.setSourceIp(fields.get("ip"));
        event.setEndpoint(fields.get("path"));
        event.setRawLine(text.length() > 1000 ? text.substring(0, 1000) : text);
        return Optional.of(event);
    }

    // Reads "user=admin ip=1.2.3.4" into a small map: {user=admin, ip=1.2.3.4}
    private Map<String, String> readFields(String text) {
        Map<String, String> fields = new HashMap<>();
        for (String part : text.split("\\s+")) {
            int equals = part.indexOf('=');
            if (equals > 0) {
                fields.put(part.substring(0, equals), part.substring(equals + 1));
            }
        }
        return fields;
    }
}