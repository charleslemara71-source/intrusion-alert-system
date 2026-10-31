package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "followingId"}))
public class Follow {
    public static final String PENDING = "PENDING", ACCEPTED = "ACCEPTED", DECLINED = "DECLINED";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long followerId;
    @Column(nullable = false) public Long followingId;
    @Column(nullable = false, length = 12) public String status = PENDING;
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime respondedAt;
}
