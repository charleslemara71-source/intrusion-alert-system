package com.ias.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ias.model.Alert;
import com.ias.model.AlertStatus;
import com.ias.repository.AlertRepository;

@Service
public class AlertService {

    // If the same rule fires for the same IP within this many minutes, it is the same attack
    private static final int MERGE_WINDOW_MINUTES = 5;

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert raise(Alert candidate) {
        Optional<Alert> open = alertRepository
                .findFirstByRuleNameAndSourceIpAndStatusOrderByLastSeenDesc(
                        candidate.getRuleName(), candidate.getSourceIp(), AlertStatus.NEW);

        boolean sameAttack = open.isPresent() && open.get().getLastSeen()
                .isAfter(candidate.getLastSeen().minusMinutes(MERGE_WINDOW_MINUTES));

        if (sameAttack) {
            // Update the existing alert instead of flooding the dashboard
            Alert existing = open.get();
            existing.setLastSeen(candidate.getLastSeen());
            existing.setEventCount(existing.getEventCount() + 1);
            existing.setTitle(candidate.getTitle());
            if (candidate.getSeverity().ordinal() > existing.getSeverity().ordinal()) {
                existing.setSeverity(candidate.getSeverity());   // only ever escalate
            }
            return alertRepository.save(existing);
        }
        return alertRepository.save(candidate);   // a brand new attack
    }
}