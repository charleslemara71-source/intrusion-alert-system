package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "comments")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long postId;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 1000) public String body;
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
