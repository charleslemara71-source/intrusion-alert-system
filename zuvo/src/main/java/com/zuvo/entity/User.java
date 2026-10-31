package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 120) public String fullName;
    @Column(nullable = false) public LocalDate dateOfBirth;
    @Column(length = 20) public String gender;
    @Column(nullable = false, unique = true, length = 160) public String email;
    @Column(nullable = false, length = 100) public String passwordHash;
    public boolean emailVerified = false;
    @Column(length = 500) public String profilePicUrl;
    @Column(length = 300) public String bio;
    @Column(nullable = false, length = 20) public String role = "USER";
    @Column(nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
}
