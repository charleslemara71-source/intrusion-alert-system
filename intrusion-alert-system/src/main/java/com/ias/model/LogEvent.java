package com.ias.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "log_event", indexes = {
        @Index(name = "idx_event_time", columnList = "event_time"),
        @Index(name = "idx_source_ip", columnList = "source_ip"),
        @Index(name = "idx_event_type", columnList = "event_type")
})
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime eventTime;   // when it happened, taken from the log line

    @Column(nullable = false, length = 50)
    private String eventType;          // LOGIN_FAILED, LOGIN_SUCCESS, ...

    @Column(length = 100)
    private String username;

    @Column(length = 45)
    private String sourceIp;

    @Column(length = 255)
    private String endpoint;

    @Column(length = 1000)
    private String rawLine;            // original text, kept as evidence

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
}