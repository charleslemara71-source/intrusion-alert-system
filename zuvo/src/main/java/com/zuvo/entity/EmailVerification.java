package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 100) public String codeHash;
    @Column(nullable = false) public LocalDateTime expiresAt;
    public int attempts = 0;
    public boolean used = false;
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
