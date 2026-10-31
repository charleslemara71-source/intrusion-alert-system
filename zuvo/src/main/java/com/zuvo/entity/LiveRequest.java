package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "live_requests", uniqueConstraints = @UniqueConstraint(columnNames = {"streamId", "userId"}))
public class LiveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long streamId;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 10) public String status = "PENDING";
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
