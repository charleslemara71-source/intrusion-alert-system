package com.ias.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ias.model.Alert;
import com.ias.model.Severity;
import com.ias.repository.AlertRepository;

@RestController
@RequestMapping("/api")
public class AlertApiController {

    private final AlertRepository alertRepository;

    public AlertApiController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public List<Alert> latestAlerts() {
        return alertRepository.findTop20ByOrderByLastSeenDesc();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", alertRepository.count());
        for (Severity severity : Severity.values()) {
            stats.put(severity.name().toLowerCase(), alertRepository.countBySeverity(severity));
        }
        return stats;
    }
}