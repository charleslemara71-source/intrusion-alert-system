package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "live_streams")
public class LiveStream {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long hostId;
    @Column(nullable = false, length = 80) public String roomName;
    @Column(length = 120) public String title;
    @Column(nullable = false, length = 10) public String status = "LIVE";
    @Column(nullable = false) public LocalDateTime startedAt = LocalDateTime.now();
    public LocalDateTime endedAt;
}
