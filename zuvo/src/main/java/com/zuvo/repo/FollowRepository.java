package com.zuvo.repo;

import com.zuvo.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<Follow> findByFollowingIdAndStatusOrderByCreatedAtDesc(Long followingId, String status);
    List<Follow> findByFollowerIdAndStatus(Long followerId, String status);
    long countByFollowingIdAndStatus(Long followingId, String status);
    long countByFollowerIdAndStatus(Long followerId, String status);
    boolean existsByFollowerIdAndFollowingIdAndStatus(Long followerId, Long followingId, String status);

    @Query("select f from Follow f where f.status = 'ACCEPTED' and (f.followerId = :u or f.followingId = :u)")
    List<Follow> findAcceptedInvolving(@Param("u") Long userId);
}
