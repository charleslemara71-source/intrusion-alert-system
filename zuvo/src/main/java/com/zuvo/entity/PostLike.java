package com.zuvo.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"postId", "userId"}))
public class PostLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long postId;
    @Column(nullable = false) public Long userId;
}
