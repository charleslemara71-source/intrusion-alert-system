package com.ias.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ias.model.Alert;
import com.ias.model.AlertStatus;
import com.ias.model.Severity;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // The 20 most recently active alerts, for the dashboard table
    List<Alert> findTop20ByOrderByLastSeenDesc();

    // Used to avoid creating duplicate alerts for the same ongoing attack
    Optional<Alert> findFirstByRuleNameAndSourceIpAndStatusOrderByLastSeenDesc(
            String ruleName, String sourceIp, AlertStatus status);

    long countBySeverity(Severity severity);
}