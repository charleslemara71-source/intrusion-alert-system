package com.zuvo.repo;

import com.zuvo.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdAndUsedFalseOrderByIdDesc(Long userId);
    Optional<EmailVerification> findTopByUserIdOrderByIdDesc(Long userId);
}
