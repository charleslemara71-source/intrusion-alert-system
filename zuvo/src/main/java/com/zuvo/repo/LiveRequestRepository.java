package com.zuvo.repo;

import com.zuvo.entity.LiveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LiveRequestRepository extends JpaRepository<LiveRequest, Long> {
    Optional<LiveRequest> findByStreamIdAndUserId(Long streamId, Long userId);
    List<LiveRequest> findByStreamIdOrderByCreatedAtAsc(Long streamId);
}
