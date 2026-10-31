package com.ias.engine;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ias.model.LogEvent;
import com.ias.service.AlertService;

/**
 * Passes every event through every rule.
 * Spring automatically gives us all classes that implement Rule.
 */
@Service
public class RuleEngine {

    private final List<Rule> rules;
    private final AlertService alertService;

    public RuleEngine(List<Rule> rules, AlertService alertService) {
        this.rules = rules;
        this.alertService = alertService;
    }

    public void run(LogEvent event) {
        for (Rule rule : rules) {
            rule.evaluate(event).ifPresent(alertService::raise);
        }
    }
}