package com.ias.engine.rules;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.ias.engine.Rule;
import com.ias.model.Alert;
import com.ias.model.AlertStatus;
import com.ias.model.LogEvent;
import com.ias.model.Severity;
import com.ias.repository.LogEventRepository;

/**
 * Threshold rule: too many failed logins from one IP in a short time
 * usually means someone is guessing passwords (brute force).
 */
@Component
public class FailedLoginThresholdRule implements Rule {

    private static final int MAX_FAILURES = 5;      // 5 or more...
    private static final int WINDOW_SECONDS = 60;   // ...within 60 seconds raises an alert

    private final LogEventRepository eventRepository;

    public FailedLoginThresholdRule(LogEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public String name() {
        return "Failed login threshold";
    }

    @Override
    public Optional<Alert> evaluate(LogEvent event) {
        // This rule only cares about failed logins that have an IP
        if (!"LOGIN_FAILED".equals(event.getEventType()) || event.getSourceIp() == null) {
            return Optional.empty();
        }

        LocalDateTime windowStart = event.getEventTime().minusSeconds(WINDOW_SECONDS);
        long failures = eventRepository.countByEventTypeAndSourceIpAndEventTimeBetween(
                "LOGIN_FAILED", event.getSourceIp(), windowStart, event.getEventTime());

        if (failures < MAX_FAILURES) {
            return Optional.empty();   // still within normal limits
        }

        Alert alert = new Alert();
        alert.setRuleName(name());
        alert.setSeverity(failures >= 10 ? Severity.HIGH : Severity.MEDIUM);
        alert.setTitle(failures + " failed logins from " + event.getSourceIp()
                + " within " + WINDOW_SECONDS + " seconds");
        alert.setDescription("Possible password-guessing attack. Latest account targeted: "
                + event.getUsername());
        alert.setSourceIp(event.getSourceIp());
        alert.setUsername(event.getUsername());
        alert.setEventCount((int) failures);
        alert.setStatus(AlertStatus.NEW);
        alert.setFirstSeen(event.getEventTime());
        alert.setLastSeen(event.getEventTime());
        return Optional.of(alert);
    }
}