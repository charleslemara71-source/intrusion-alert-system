package com.farm.workersystem.dto;

import com.farm.workersystem.model.Role;
import com.farm.workersystem.model.Worker;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkerResponse(
        Long id,
        String fullName,
        String idNumber,
        String email,
        Role role,
        String section,
        BigDecimal salary,
        LocalDateTime registeredAt) {

    public static WorkerResponse from(Worker w) {
        return new WorkerResponse(w.getId(), w.getFullName(), w.getIdNumber(),
                w.getEmail(), w.getRole(), w.getSection(), w.getSalary(), w.getRegisteredAt());
    }
}