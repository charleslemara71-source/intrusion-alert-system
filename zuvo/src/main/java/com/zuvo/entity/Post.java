package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "posts")
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 10) public String type;
    @Column(length = 600) public String mediaUrl;
    @Column(length = 300) public String mediaPublicId;
    @Column(length = 20) public String resourceType;
    @Column(length = 1000) public String caption;
    @Column(length = 30) public String category;
    @Column(nullable = false, length = 12) public String visibility = "FOLLOWERS";
    public boolean curated = false;
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
