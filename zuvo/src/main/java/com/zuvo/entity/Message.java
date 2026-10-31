package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long senderId;
    @Column(nullable = false) public Long receiverId;
    @Column(nullable = false, length = 2000) public String content;
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
    public boolean seen = false;
}
