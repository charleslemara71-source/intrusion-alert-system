package com.farm.workersystem.repository;

import com.farm.workersystem.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.farm.workersystem.model.Role;
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    boolean existsByEmail(String email);
    boolean existsByIdNumber(String idNumber);
    Optional<Worker> findByEmail(String email);
    boolean existsByRole(Role role);
}