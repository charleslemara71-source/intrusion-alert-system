package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "user_interests")
public class UserInterest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 30) public String category;
    public UserInterest() {}
    public UserInterest(Long userId, String category) { this.userId = userId; this.category = category; }
}
