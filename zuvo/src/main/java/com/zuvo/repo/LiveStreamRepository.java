package com.zuvo.repo;

import com.zuvo.entity.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {
    List<LiveStream> findByStatusOrderByStartedAtDesc(String status);
    Optional<LiveStream> findFirstByHostIdAndStatus(Long hostId, String status);
}
