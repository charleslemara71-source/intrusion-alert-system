package com.ias.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ias.model.LogEvent;

public interface LogEventRepository extends JpaRepository<LogEvent, Long> {

    // "How many events of this type came from this IP between these two times?"
    long countByEventTypeAndSourceIpAndEventTimeBetween(
            String eventType, String sourceIp, LocalDateTime from, LocalDateTime to);
}